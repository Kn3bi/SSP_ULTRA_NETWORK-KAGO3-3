package my_project.view;

import KAGO_framework.control.ViewController;
import KAGO_framework.model.GraphicalObject;
import KAGO_framework.view.DrawTool;
import KAGO_framework.view.ProgramView;
import KAGO_framework.view.simple_gui.GIFPainter;
import my_project.control.ProgramController;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class PlayView extends ProgramView {

    private GIFPainter background, arrow;
    private GraphicalObject[] gameIcons;
    private BufferedImage cross, waiting, fightingAgainst, chooseNow;
    private BufferedImage[] playerImageIcons;

    private int playerIconIndex, enemyIconIndex;
    private int state = 0; // 0 => Auswahl zum testen || 1 => nächste Auswahl gilt
    private int selectedIndex = -1;
    private int selectedEnemyChoiceIndex = -1;
    private String enemyName = null, playername;
    private boolean mayChoose = true;
    private int playerPoints;
    private RoundAnimation currentAni;
    private double aniTimer;
    private String statusDisplay = "Erwarte Status...";

    private String remainingTime = "";

    public enum RoundAnimation{
        NONE, WINNING, DRAW, LOOSING;
    }

    /**
     * Erzeugt ein Objekt der Klasse GameView
     *
     * @param viewController    das ViewController-Objekt des Frameworks
     * @param programController das ProgramController-Objekt des Frameworks
     */
    public PlayView(ViewController viewController, ProgramController programController, BufferedImage[] playerImageIcons, int playerIconIndex, String playername, int playerPoints) {
        super(viewController, programController);
        currentAni = RoundAnimation.NONE;
        this.playerImageIcons = playerImageIcons;
        this.playerIconIndex = playerIconIndex;
        this.playername = playername;
        this.playerPoints = playerPoints;
        enemyIconIndex = -1;
        viewController.getSoundController().stopSound("title");
        viewController.getSoundController().loadSound("assets/sounds/battle.mp3","battle",true);
        viewController.getSoundController().playSound("battle");
        background = new GIFPainter("assets/images/background3.gif",0,0);
        arrow = new GIFPainter("assets/images/gameIcons/arrow.gif",100,200);
        gameIcons = new GraphicalObject[5];
        cross = this.createImage("assets/images/gameIcons/redCross.png");
        gameIcons[0] = new GraphicalObject("assets/images/gameIconsText/sword.png");
        gameIcons[1] = new GraphicalObject("assets/images/gameIconsText/pawn.png");
        gameIcons[2] = new GraphicalObject("assets/images/gameIconsText/hunger.png");
        gameIcons[3] = new GraphicalObject("assets/images/gameIconsText/king.png");
        gameIcons[4] = new GraphicalObject("assets/images/gameIconsText/cake.png");
        for(int i = 0; i < 5; i++){
            gameIcons[i].setX(25+i*110);
            gameIcons[i].setY(30);
        }
        waiting = this.createImage("assets/images/waiting.png");
        chooseNow = this.createImage("assets/images/chooseNow.png");
        fightingAgainst = this.createImage("assets/images/fightingAgainst.png");
    }

    @Override
    public void draw(DrawTool drawTool) {
        background.draw(drawTool);
        drawTool.setCurrentColor(180,180,180,180);
        drawTool.drawFilledRectangle(gameIcons[0].getX()-10,gameIcons[0].getY()-10,5*110+10,gameIcons[0].getHeight()+30);
        for(int i = 0; i < 5; i++){
            gameIcons[i].draw(drawTool);
        }
        drawTool.setCurrentColor(255,255,0,255);
        for(int i = 0; i<5; i++){
            if(selectedIndex == i) {
                //drawTool.drawRectangle(gameIcons[i].getX()-2,gameIcons[i].getY()-2,gameIcons[i].getWidth()+4,gameIcons[i].getHeight()+4);
                arrow.setX(gameIcons[i].getX());
                arrow.setY(gameIcons[i].getY()+gameIcons[i].getHeight()-20);
                arrow.draw(drawTool);
                double x = gameIcons[(i+1) % 5].getX();
                double y = gameIcons[(i+1) % 5].getY();
                drawTool.drawImage(cross,x,y);
                x = gameIcons[(i+3) % 5].getX();
                y = gameIcons[(i+3) % 5].getY();
                drawTool.drawImage(cross,x,y);
            }
        }
        if(!mayChoose){
            drawTool.setCurrentColor(50,50,50,120);
            drawTool.drawFilledRectangle(gameIcons[0].getX()-10,gameIcons[0].getY()-10,5*110+10,gameIcons[0].getHeight()+30);
        }
        if(enemyIconIndex != -1){
            drawTool.drawImage(playerImageIcons[enemyIconIndex],550-playerImageIcons[enemyIconIndex].getWidth(),300);
            drawTool.formatText("Purisa", Font.BOLD,16);
            drawTool.setCurrentColor(255,0,0,255);
            drawTool.drawText((550-playerImageIcons[enemyIconIndex].getWidth()*0.5)+10-4.85*enemyName.length(),290,enemyName);
        }
        if(state == 0){
            drawTool.drawImage(waiting, 300-waiting.getWidth()/2, 300);
        }
        if(state == 1){
            if(mayChoose) drawTool.drawImage(chooseNow, 300-chooseNow.getWidth()/2, 180);
            drawTool.drawImage(fightingAgainst, 300-fightingAgainst.getWidth()/2, 300);
            if(selectedIndex != -1){
                drawTool.drawImage(gameIcons[selectedIndex].getMyImage(),150,375);
            }
            if(selectedEnemyChoiceIndex != -1){
                drawTool.drawImage(gameIcons[selectedEnemyChoiceIndex].getMyImage(),450-gameIcons[selectedEnemyChoiceIndex].getWidth(),375);
            } else {
                //drawTool.drawText(450-gameIcons[0].getWidth(),380,"???");
            }
        }
        // Statustexte
        drawTool.setCurrentColor(0,255,0,255);
        drawTool.drawImage(playerImageIcons[playerIconIndex],50,300);
        drawTool.formatText("Purisa", Font.BOLD,16);
        drawTool.drawText((50+playerImageIcons[playerIconIndex].getWidth()*0.5)-4.85*playername.length(),290,playername);
        drawTool.setCurrentColor(255,255,255,255);
        drawTool.drawText(225,520,"Deine Punkte: "+playerPoints);
        drawTool.formatText("Arial", Font.PLAIN,11);
        drawTool.setCurrentColor(255,255,0,255);
        drawTool.drawText(10,575,statusDisplay);
        if(programController.isAussetzen() || this.state == 1){
            drawTool.drawText(10,550,remainingTime);
            if(programController.isAussetzen()) {
                drawTool.formatText("Arial", Font.BOLD, 40);
                drawTool.setCurrentColor(255, 255, 255, 255);
                drawTool.drawText(180, 250, "Aussetzen...");
            }
        }
        // Animationen zum Rundenende (noch primitiv)
        if (currentAni == RoundAnimation.DRAW){
            drawTool.formatText("Arial", Font.BOLD,48);
            randomizeColor(drawTool);
            drawTool.drawText(220,300,"DRAW!");
        }
        if (currentAni == RoundAnimation.WINNING){
            drawTool.formatText("Arial", Font.BOLD,48);
            randomizeColor(drawTool);
            drawTool.drawText(180,300,"YOU WIN!!");
        }
        if (currentAni == RoundAnimation.LOOSING){
            drawTool.formatText("Arial", Font.BOLD,48);
            randomizeColor(drawTool);
            drawTool.drawText(170,300,"YOU LOSE!");
        }
    }

    private void randomizeColor(DrawTool drawTool){
        drawTool.setCurrentColor((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random()),255);
    }

    public void setNextEnemy(String enemyName){
        this.enemyName = enemyName;
        if(this.enemyName.length() >= 10) this.enemyName = this.enemyName.substring(0,9);
        int random = (int)(Math.random()*5);
        enemyIconIndex = (playerIconIndex + random) % 5;
    }

    public void activateChoosing(){
        state = 1;
        mayChoose = true;
        if(currentAni == RoundAnimation.NONE) selectedIndex = -1;
    }

    public void setEnemyChoice(int index){
        selectedEnemyChoiceIndex = index;
    }

    public void setPlayerPoints(int p){
        playerPoints = p;
    }

    public void concludeRound(RoundAnimation ani){
        this.currentAni = ani;
        aniTimer = 5;
    }

    @Override
    public void keyReleased(int key) {
        super.keyReleased(key);
        if(key == KeyEvent.VK_N){
            setNextEnemy("Lisa");
            activateChoosing();
            setEnemyChoice(2);
        }
        if(key == KeyEvent.VK_X){
            programController.setState(ProgramController.State.FINISHED);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        for (int i = 0; i < 5; i++) {
            if (gameIcons[i].collidesWith(x,y) && currentAni == RoundAnimation.NONE) {
                if (selectedIndex != i && mayChoose) {
                    selectedIndex = i;
                    viewController.getSoundController().playSound("unsheathe");
                    if(state == 1){
                        programController.sendSelectionToServer(selectedIndex);
                        mayChoose = false;
                    }
                }
            }
        }
    }

    @Override
    public void update(double dt){
        viewController.getSoundController().setVolume("battle",0.25);
        if(currentAni != RoundAnimation.NONE && aniTimer <= 0){
            selectedIndex = -1;
            selectedEnemyChoiceIndex = -1;
            enemyIconIndex = -1;
            enemyName = "<Unknown Player>";
            currentAni = RoundAnimation.NONE;
        }
        if(currentAni != RoundAnimation.NONE && aniTimer > 0){
            aniTimer -= dt;
            //System.out.println("Time: "+aniTimer);
        }
    }

    public void setRemainingTime(String remainingTime) {
        this.remainingTime = remainingTime;
    }

    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
    }

    public void disposeView(){
        super.disposeView();
        viewController.getSoundController().stopSound("battle");
    }
}
