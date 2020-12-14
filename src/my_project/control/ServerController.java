package my_project.control;

import KAGO_framework.control.Drawable;
import KAGO_framework.control.ViewController;
import KAGO_framework.model.abitur.datenstrukturen.Queue;
import KAGO_framework.model.abitur.netz.Server;
import KAGO_framework.view.DrawTool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

public class ServerController extends Server implements Drawable {

    private class Player implements Comparable<Player>{

        Queue<Player> battleOpponents;
        Player currentEnemy;

        boolean isFighting;
        String name;
        String ip;
        int port;
        int points = 0;
        int lastChoice = -1;

        Player(String ip, int port){
            name = null;
            isFighting = false;
            currentEnemy = null;
            battleOpponents = new Queue<>();
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Player player = (Player) o;
            return ip.equals(player.ip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, port);
        }

        @Override
        public int compareTo(Player o) {
            if(this.points > o.points) return 1;
            if(this.points < o.points) return -1;
            return 0;
        }
    }

    private enum Gamestate {
        WAITINGFORPLAYERS, PLAYINGROUND, WAITING, CONCLUSION
    }

    private double gameTimer, lastFullSecond;

    private ArrayList<Player> players;
    private ProgramController programController;
    private Gamestate gamestate;

    public ServerController(int pPort, ViewController viewController, ProgramController programController) {
        super(pPort);
        restartServer(false);
        viewController.draw(this);
        this.programController = programController;
    }

    private void restartServer(boolean keepConnections){
        if(!keepConnections) players = new ArrayList<>();
        gamestate = Gamestate.WAITINGFORPLAYERS;
        gameTimer = 20;
        lastFullSecond = 20;
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        Player newPlayer = new Player(pClientIP,pClientPort);
        if(gamestate == Gamestate.WAITINGFORPLAYERS) {
            if (!players.contains(newPlayer)) {
                System.out.println("Server-Status: Neuen Spieler hinzugefügt (Nr."+players.size()+"): "+newPlayer.ip+" => erbitte Name.");
                players.add(newPlayer);
            } else {
                send(pClientIP, pClientPort, "ERROR$Du bist schon angemeldet.");
            }
            send(pClientIP, pClientPort, "sende$name");
        } else {
            send(pClientIP, pClientPort, "ERROR$Die Runde läuft schon.");
        }
    }

    private void printAllPlayers(){
        System.out.println("Server-Status: all current players:");
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()){
            Player temp = iterator.next();
            System.out.println("   >"+players.indexOf(temp)+": "+temp.ip+":"+temp.port+" => name: "+temp.name);
        }
        System.out.println("");
    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        Iterator<Player> iterator = players.iterator();
        Player currentPlayer = null;
        while(iterator.hasNext() && currentPlayer == null){
            Player temp = iterator.next();
            if (temp.ip.equals(pClientIP)){
                currentPlayer = temp;
            }
        }
        if(currentPlayer == null){
            send(pClientIP,pClientPort,"ERROR$Du bist unbekannt.");
        } else {
            String[] tokens = pMessage.split("\\$");
            // NAME SETZEN
            if(currentPlayer.name == null) {
                if (tokens[0].equals("name")) {
                    if (tokens[1] != null){
                        currentPlayer.name = tokens[1];
                        System.out.println("Server-Status: Name von Spieler (Nr."+(players.indexOf(currentPlayer)+1)+") erhalten: "+tokens[1]);
                        printAllPlayers();
                    }
                } else {
                    send(pClientIP, pClientPort, "ERROR$Leere Namen sind nicht erlaubt.");
                }
            } else {
                send(pClientIP, pClientPort, "ERROR$Du hat bereits einen Namen gewählt.");
            }
            // WAHL SPIELEN
            if(tokens[0].equals("spiele") && gamestate == Gamestate.PLAYINGROUND){
                if(currentPlayer.lastChoice == -1 && currentPlayer.isFighting) {
                    int wahl = programController.convertLetterToNumber(tokens[1]);
                    if (wahl != -1) {
                        currentPlayer.lastChoice = wahl;
                        System.out.println("Server-Status: Wahl von Spieler (Nr."+(players.indexOf(currentPlayer)+1)+") erhalten: "+tokens[1]);
                    } else {
                        send(pClientIP, pClientPort, "ERROR$Die Wahl muss A,B,C,D oder E sein.");
                    }
                } else{
                    send(pClientIP, pClientPort, "ERROR$Du hast schon gewählt oder kämpfst gerade nicht.");
                }
            }
        }
    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()) {
            Player currentPlayer =iterator.next();
            if(currentPlayer.ip.equals(pClientIP) && currentPlayer.port == pClientPort) {
                System.out.println("Server-Status: Player disconnected: "+pClientIP);

                iterator.remove();
                if(gamestate != Gamestate.WAITINGFORPLAYERS){
                    if (!(gamestate == Gamestate.WAITING && players.size() >= 2)){
                        // Es sind weniger als zwei Spieler in der Lobby oder das Spiel läuft gerade
                        sendToAll("ERROR$Ein Spieler hat das laufende Match verlassen: Neustart wegen Ragequit");
                        restartServer(true);
                    }
                }
            }
        }
    }

    @Override
    public void draw(DrawTool drawTool) {

    }

    @Override
    public void update(double dt) {
        if(gamestate == Gamestate.WAITINGFORPLAYERS){
            if (players.size() >= 2 && players.get(0).name!=null && players.get(1).name!=null){
                gameTimer -= dt;
                if(gameTimer <= 0){
                    generateTournament();
                    sendPoints();
                    startNewRound();
                }
                sendGametimer();
            }
        }
        if(gamestate == Gamestate.PLAYINGROUND){
            gameTimer -= dt;
            sendGametimer();
            if(readyForRoundEnd() || gameTimer <= 0){
                gameTimer = 4;
                concludeRound();
                sendPoints();
                gamestate = Gamestate.WAITING;
            }
        }
        if(gamestate == Gamestate.WAITING){
            gameTimer -= dt;
            if(gameTimer <= 0){
                if(areBattlesRemaining()){
                    startNewRound();
                    gamestate = Gamestate.PLAYINGROUND;
                } else {
                    concludeTournament();
                    sendPoints();
                    gamestate = Gamestate.CONCLUSION;
                    gameTimer = 10;
                }
            }
        }
        if(gamestate == Gamestate.CONCLUSION){
            //nyi
        }
    }

    private void startNewRound(){
        createBattles();
        sendEnemies();
        gameTimer = 20;
    }

    private void sendGametimer(){
        if(lastFullSecond > Math.floor(gameTimer)){
            sendToAll("zeit$"+(int)Math.floor(gameTimer));
            lastFullSecond = Math.floor(gameTimer);
        }
    }

    private void generateTournament(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()) {
            Player currentPlayer =iterator.next();
            if(currentPlayer.name == null) {
                send(currentPlayer.ip, currentPlayer.port, "status$rausgeworfen$Du hast keinen Namen gewählt. Ciao!");
                closeConnection(currentPlayer.ip, currentPlayer.port);
            }
        }
        iterator = players.iterator();
        while(iterator.hasNext()){
            Iterator<Player> innerIterator = players.iterator();
            Player currentPlayer =iterator.next();
            currentPlayer.lastChoice = -1;
            currentPlayer.points = 0;
            currentPlayer.battleOpponents = new Queue<>();
            currentPlayer.currentEnemy = null;
            while(innerIterator.hasNext()) {
                Player possibleOp = innerIterator.next();
                if(currentPlayer != possibleOp){
                    currentPlayer.battleOpponents.enqueue(possibleOp);
                }
            }
        }
    }

    private void sendPoints(){
        String points = "punkte$";
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()) {
            Player currentPlayer = iterator.next();
            points = points+currentPlayer.name+"$"+currentPlayer.points;
            if(iterator.hasNext()) points += "$";
        }
        sendToAll(points);
    }

    private void createBattles(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()){
            Player currentPlayer = iterator.next();
            currentPlayer.lastChoice = -1;
            currentPlayer.isFighting = false;
            currentPlayer.currentEnemy = null;
        }
        iterator = players.iterator();
        while(iterator.hasNext()){
            Player currentPlayer = iterator.next();
            if(!currentPlayer.isFighting){
                currentPlayer.isFighting = true;
                if(!currentPlayer.battleOpponents.front().isFighting && !currentPlayer.battleOpponents.isEmpty()){
                    currentPlayer.currentEnemy = currentPlayer.battleOpponents.front();
                    currentPlayer.battleOpponents.dequeue();
                    currentPlayer.currentEnemy.isFighting = true;
                    currentPlayer.currentEnemy.currentEnemy = currentPlayer;
                } else {
                    currentPlayer.currentEnemy = null; //Aussetzen
                }
            }
        }
    }

    private void sendEnemies(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()) {
            Player currentPlayer = iterator.next();
            if(currentPlayer.currentEnemy != null) {
                send(currentPlayer.currentEnemy.ip, currentPlayer.currentEnemy.port, "gegner$name$" + currentPlayer.name);
            }else { //Aussetzen
                send(currentPlayer.ip,currentPlayer.port,"status$aussetzen");
            }
        }
    }

    private boolean readyForRoundEnd(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()){
            Player currentPlayer = iterator.next();
            if(currentPlayer.lastChoice == -1 && currentPlayer.currentEnemy != null) return false;
        }
        return true;
    }

    private void concludeRound(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()) {
            Player currentPlayer = iterator.next();
            if(currentPlayer.isFighting){
                if(currentPlayer.lastChoice == -1) {
                    send(currentPlayer.ip, currentPlayer.port, "status$rausgeworfen$Du hast nichts gewählt. Ciao!");
                    closeConnection(currentPlayer.ip, currentPlayer.port);
                    if (currentPlayer.currentEnemy != null) {
                        currentPlayer.currentEnemy.isFighting = false;
                        send(currentPlayer.currentEnemy.ip, currentPlayer.currentEnemy.port, "status$ausgang$unentschieden");
                    }
                    iterator.remove();
                } else {
                    if(currentPlayer.currentEnemy.lastChoice != -1){
                        concludeMatch(currentPlayer,currentPlayer.currentEnemy);
                    }
                }
            }
        }
    }

    private void concludeMatch(Player p1, Player p2){
        p1.isFighting = false;
        p2.isFighting = false;
        send(p1.ip,p1.port,"gegner$auswahl$"+programController.convertNumberToLetter(p2.lastChoice));
        send(p2.ip,p2.port,"gegner$auswahl$"+programController.convertNumberToLetter(p1.lastChoice));
        if(p1.lastChoice == p2.lastChoice){
            send(p1.ip,p1.port,"status$ausgang$unentschieden");
            send(p2.ip,p2.port,"status$ausgang$unentschieden");
            p1.points += 1;
            p2.points += 1;
        } else {
            if(winsAgainst(p1.lastChoice,p2.lastChoice)){
                send(p1.ip,p1.port,"status$ausgang$gewonnen");
                p1.points += 3;
                send(p2.ip,p2.port,"status$ausgang$verloren");
                p2.points -= 1;
            } else {
                send(p1.ip,p1.port,"status$ausgang$verloren");
                p1.points -= 1;
                send(p2.ip,p2.port,"status$ausgang$gewonnen");
                p2.points += 3;
            }
        }
    }

    private boolean winsAgainst(int c1, int c2){
        if((c1+1) % 5 == c2 || (c1+3) % 5 == c2) return true;
        return false;
    }

    private boolean areBattlesRemaining(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()) {
            if(!iterator.next().battleOpponents.isEmpty()) return true;
        }
        return false;
    }

    private void concludeTournament(){
        players.sort(Comparator.naturalOrder());
    }


}
