package my_project.control;

import KAGO_framework.control.Drawable;
import KAGO_framework.control.ViewController;
import KAGO_framework.model.abitur.netz.Server;
import KAGO_framework.view.DrawTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class ServerController extends Server implements Drawable {

    private class Player{

        String name = null;
        String ip;
        int port;
        int points = 0;
        int lastChoice = -1;

        Player(String ip, int port){
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Player player = (Player) o;
            return port == player.port &&
                    ip.equals(player.ip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, port);
        }
    }

    private enum Gamestate {
        WAITINGFORPLAYERS, STARTING, INROUND, AFTERROUND, CONCLUSION
    }

    private double gameTimer;

    private ArrayList<Player> players;
    private Player[][] battleRoster;
    private int roundsToPlay, currentRound;
    private Gamestate gamestate;

    public ServerController(int pPort, ViewController viewController) {
        super(pPort);
        players = new ArrayList<>();
        gamestate = Gamestate.WAITINGFORPLAYERS;
        gameTimer = 20;
        battleRoster = null;
        roundsToPlay = -1;
        currentRound = -1;
        viewController.draw(this);
    }

    @Override
    public void processNewConnection(String pClientIP, int pClientPort) {
        Player newPlayer = new Player(pClientIP,pClientPort);
        if(gamestate == Gamestate.WAITINGFORPLAYERS) {
            if (!players.contains(newPlayer)) {
                players.add(newPlayer);
            } else {
                send(pClientIP, pClientPort, "ERROR$Du bist schon angemeldet.");
            }
            send(pClientIP, pClientPort, "sende$name");
        } else {
            send(pClientIP, pClientPort, "ERROR$Die Runde läuft schon.");
        }
    }

    @Override
    public void processMessage(String pClientIP, int pClientPort, String pMessage) {
        Iterator<Player> iterator = players.iterator();
        Player currentPlayer = null;
        while(iterator.hasNext() && !currentPlayer.ip.equals(pClientIP)){
            currentPlayer = iterator.next();
        }
        if(currentPlayer == null){
            send(pClientIP,pClientPort,"ERROR$Du bist unbekannt.");
        } else {
            String[] tokens = pMessage.split("\\$");
            // NAME SETZEN
            if(currentPlayer.name == null) {
                if (tokens[0].equals("name")) {
                    if (tokens[1] != null) currentPlayer.name = tokens[1];
                } else {
                    send(pClientIP, pClientPort, "ERROR$Leere Namen sind nicht erlaubt.");
                }
            } else {
                send(pClientIP, pClientPort, "ERROR$Du hat bereits einen Namen gewählt.");
            }
            // WAHL SPIELEN
            if(tokens[0].equals("spiele")){
                if(currentPlayer.lastChoice == -1) {
                    int wahl = convertLetterToNumber(tokens[1]);
                    if (wahl != -1) {
                        currentPlayer.lastChoice = wahl;
                    } else {
                        send(pClientIP, pClientPort, "ERROR$Die Wahl muss A,B,C,D oder E sein.");
                    }
                } else{
                    send(pClientIP, pClientPort, "ERROR$Du hast schon gewählt.");
                }
            }
        }
    }

    @Override
    public void processClosingConnection(String pClientIP, int pClientPort) {

    }

    private int convertLetterToNumber(String a){
        switch (a){
            case "A": return 0;
            case "B": return 1;
            case "C": return 2;
            case "D": return 3;
            case "E": return 4;
        }
        return -1;
    }

    private String convertNumberToLetter(int a){
        switch(a){
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            case 4: return "E";
        }
        return "";
    }

    private boolean allPlayersValid(){
        Iterator<Player> iterator = players.iterator();
        while(iterator.hasNext()){
            if (iterator.next().name == null) return false;
        }
        return true;
    }


    @Override
    public void draw(DrawTool drawTool) {

    }

    @Override
    public void update(double dt) {
        if(gamestate == Gamestate.WAITINGFORPLAYERS){
            if (players.size() >= 2 && allPlayersValid()){
                gameTimer -= dt;
                if(gameTimer <= 0){
                    gameTimer = 0;
                    currentRound = 0;
                    roundsToPlay = players.size()-1;
                    generateRoster();
                    gamestate = Gamestate.STARTING;
                }
                sendToAll("zeit$"+Math.floor(gameTimer));
            } else {
                sendToAll("zeit$0");
            }
        }
    }

    private void generateRoster(){
        currentRound ++;
        battleRoster = new Player[players.size()/2][2];
        // Create some roster
    }


}
