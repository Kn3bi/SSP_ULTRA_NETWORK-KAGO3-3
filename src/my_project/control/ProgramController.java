package my_project.control;

import KAGO_framework.control.ViewController;
import my_project.model.Player;
import my_project.view.ConclusionView;
import my_project.view.PlayView;
import my_project.view.PlayerSelectView;
import my_project.view.StartView;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * Ein Objekt der Klasse ProgramController dient dazu das Programm zu steuern. Die updateProgram - Methode wird
 * mit jeder Frame im laufenden Programm aufgerufen.
 */
public class ProgramController {

    //Attribute
    private int targetPort;
    private int currentTimerValue;
    private boolean aussetzen;

    // Referenzen
    private ViewController viewController;  // diese Referenz soll auf ein Objekt der Klasse viewController zeigen. Über dieses Objekt wird das Fenster gesteuert.
    private SSPNetworkController networkController;
    private StartView startView;
    private PlayerSelectView playerSelectView;
    private PlayView playView;
    private ConclusionView conclusionView;
    private State state;
    private Player player;
    private String[] lastRanking;

    // Enum
    public enum State {
        SCANNING, TRYAGAIN, PLAYERSELECT, WAITINGFORNAME, PLAYING, FINISHED
    }

    /**
     * Konstruktor
     * Dieser legt das Objekt der Klasse ProgramController an, das den Programmfluss steuert.
     * Damit der ProgramController auf das Fenster zugreifen kann, benötigt er eine Referenz auf das Objekt
     * der Klasse viewController. Diese wird als Parameter übergeben.
     * @param ViewController das viewController-Objekt des Programms
     */
    public ProgramController(ViewController ViewController){
        this.viewController = ViewController;
    }

    /**
     * Diese Methode wird genau ein mal nach Programmstart aufgerufen. Achtung: funktioniert nicht im Szenario-Modus
     */
    public void startProgram() {
        networkController = new SSPNetworkController(this);
        while(targetPort < 1000 || targetPort > 65000) {
            try {
                String m = JOptionPane.showInputDialog("Port für Spielserver (1000 bis 65000) eingeben:");
                targetPort = Integer.parseInt(m);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(viewController.getDrawFrame(), "Wer keine Zahl eingeben kann, darf nicht spielen.", "Braincheck", JOptionPane.ERROR_MESSAGE);
            }
        }
        restart();
    }

    private void restart(){
        if(playView != null) playView.disposeView();
        if(startView != null) startView.disposeView();
        if(playerSelectView != null) playerSelectView.disposeView();
        if(conclusionView != null) conclusionView.disposeView();
        lastRanking = new String[3];
        lastRanking[0] = "Bisher kein";
        lastRanking[1] = "Ranking erhalten";
        lastRanking[2] = "-1";
        currentTimerValue = 0;
        aussetzen = false;
        startView = new StartView(viewController,this);
        setState(State.SCANNING);
    }

    /**
     * Sorgt dafür, dass zunächst gewartet wird, damit der SoundController die
     * Initialisierung abschließen kann. Die Wartezeit ist fest und damit nicht ganz sauber
     * implementiert, aber dafür funktioniert das Programm auch bei falscher Java-Version
     * @param dt Zeit seit letzter Frame
     */
    public void updateProgram(double dt){
        if(state == State.SCANNING){
            if (networkController.getServerIP() != null){
                if (networkController.getServerIP().equals("timeout")){
                    startView.displayTryAgain();
                    setState(State.TRYAGAIN);
                } else {
                    setState(State.PLAYERSELECT);
                }
            } else {
                startView.displayScanning();
            }
        }
        if(state == State.PLAYING || state == State.WAITINGFORNAME){
            displayTimer();
        }
    }

    /**
     * Ändert den Spielzusand
     * @param state ein im enum State definierter Zustand
     */
    public void setState(State state){
        this.state = state;
        if (state == State.SCANNING) networkController.startNetworkScan(targetPort);
        if (state == State.PLAYERSELECT){
            startView.disposeView();
            networkController.startConnection();
            playerSelectView = new PlayerSelectView(viewController,this);
        }
        if (state == State.PLAYING){
            player = new Player(playerSelectView.getName());
            playerSelectView.disposeView();
            playView = new PlayView(viewController,this,playerSelectView.getPlayerIcons(),playerSelectView.getSelectedIconIndex(),player.getName(),player.getPunkte());
            networkController.sendPlayerName(player);
       }
        if(state == State.FINISHED){
            playView.disposeView();
            if(lastRanking[1].equals(player.getName())){
                conclusionView = new ConclusionView(viewController,this,playerSelectView.getPlayerIcons()[playerSelectView.getSelectedIconIndex()],lastRanking, ConclusionView.Conclusion.WIN);
            } else {
                conclusionView = new ConclusionView(viewController,this,playerSelectView.getPlayerIcons()[playerSelectView.getSelectedIconIndex()],lastRanking, ConclusionView.Conclusion.LOSE);
            }
        }
    }

    public void mouseClicked(MouseEvent e){}

    public void sendSelectionToServer(int selectedIndex){
        networkController.sendPlayerChoice(convertNumberToLetter(selectedIndex));
    }

    public void requestSelectionFromPlayer(){
        aussetzen = false;
        playView.activateChoosing();
    }

    public void verarbeiteGegnernamen(String name){
        playView.setNextEnemy(name);
    }

    public void verarbeiteGegnerauswahl(String auswahl){
        playView.setEnemyChoice(convertLetterToNumber(auswahl));
    }

    public void verarbeiteNeuePunkte(String[] tokens){
        lastRanking = tokens;
        try{
            String punkte = "-1"; // Fehlerwert
            String status = "Punktestand --- ";
            for(int i = 0; i < tokens.length; i++){
                if(tokens[i].equals(player.getName())){
                    punkte = tokens[i+1];
                    playView.setPlayerPoints(Integer.parseInt(punkte));
                }
                if(i>0 & i%2 == 1){
                    status = status + tokens[i] + ": "+tokens[i+1]+" ";
                }
            }
            int newPoints = Integer.parseInt(punkte);
            player.setPunkte(newPoints);
            playView.setPlayerPoints(newPoints);
            playView.setStatusDisplay(status);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public State getState() {
        return state;
    }

    public int convertLetterToNumber(String a){
        switch (a){
            case "A": return 0;
            case "B": return 1;
            case "C": return 2;
            case "D": return 3;
            case "E": return 4;
        }
        return -1;
    }

    public String convertNumberToLetter(int a){
        switch(a){
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            case 4: return "E";
        }
        return "";
    }

    public void verarbeiteNeuenTimer(String timer){
        try{
            currentTimerValue = Integer.parseInt(timer);
        } catch (NumberFormatException e) {
            currentTimerValue = -1;
        }
    }

    private void displayTimer(){
        String msg = "";
        if (currentTimerValue <= 0){
            msg = "Momentan kein Zeitlimit";
        } else {
            if(aussetzen){
                msg = "Maximale Wartezeit bis du dran bist: "+currentTimerValue+ "s";
            }else{
                msg = "Zeit für deine Entscheidung: "+currentTimerValue+ "s";
            }
        }
        if (playerSelectView != null) playerSelectView.setRemainingTime(msg);
        if (playView != null){
            playView.setRemainingTime(msg);
        }
    }

    public void verarbeiteNeuenStatus(String[] status){
        if(status[1].equals("aussetzen")){
            aussetzen = true;
        }
        if(status[1].equals("rausgeworfen")){
            String reason = status[2];
            JOptionPane.showMessageDialog(viewController.getDrawFrame(),"Grund für deinen Rauswurf: "+reason,"Der Server hat dich rausgeworfen!",JOptionPane.WARNING_MESSAGE);
            restart();
        }
        if(status[1].equals("spielende")){
            this.setState(State.FINISHED);
        }
        if(status[1].equals("ausgang")){
            if(status[2].equals("gewonnen")){
                playView.concludeRound(PlayView.RoundAnimation.WINNING);
            } else if (status[2].equals("verloren")){
                playView.concludeRound(PlayView.RoundAnimation.LOOSING);
            } else if (status[2].equals("unentschieden")){
                playView.concludeRound(PlayView.RoundAnimation.DRAW);
            }
        }
    }

    public boolean isAussetzen(){
        return aussetzen;
    }

    public void quickConnect(){
        String ip = JOptionPane.showInputDialog("Bitte IP-Adresse eingeben:");
        try {
            this.networkController.startConnection(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
