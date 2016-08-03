import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
//import com.firebase.client.ValueEventListener;


public class DrawPanel extends JPanel implements ActionListener{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private Firebase myFirebaseRef;
    Font font = new Font("SegoePrint", Font.BOLD, 35);
    Timer timerGame = new Timer(1000, this);
    Timer timerPause = new Timer(1000,this);
    String scrambledWord;
    private int roundSeconds;
    private int pauseSeconds;
    private int countWinners;
    private boolean isGameStarted;
    private boolean preventPlayersToConnect;
    private boolean newGame = false;
    private Vector<User> users;
    Map<String, Integer> userNameIDMap = new HashMap<String, Integer>();
    private String currentWord;
    private Random random;
    private int numOfRounds;
    private int gameState;
    private int pointsForWin;
    private Color[] colors = {Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW, Color.CYAN, Color.DARK_GRAY};

    private BufferedImage lobbyImage;
    private BufferedImage gamePlayImage;
    private BufferedImage nextRoundImage;
    private BufferedImage resultsImage;

    public DrawPanel() {

        this.users = new Vector<User>();
        random = new Random();
        numOfRounds = 0;
        countWinners = 0;
        roundSeconds = 100;
        pauseSeconds = 5;
        isGameStarted = false;
        preventPlayersToConnect = false;
        pointsForWin = 10;
        myFirebaseRef = new Firebase("https://funwithwords.firebaseio.com/");
        myFirebaseRef.removeValue(); //Cleans out everything


        try {
            lobbyImage = ImageIO.read(new File("lobby.png"));
            nextRoundImage = ImageIO.read(new File("nextRound.png"));
            gamePlayImage = ImageIO.read(new File("gamePlay.png"));
            resultsImage = ImageIO.read(new File("results.png"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        myFirebaseRef.child("ScreenNbr").setValue(Constants.screenNbr);
        myFirebaseRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildRemoved(DataSnapshot arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onChildMoved(DataSnapshot arg0, String arg1) {
                // TODO Auto-generated method stub

            }

            @Override
            public synchronized void onChildChanged(DataSnapshot arg0, String arg1) {

                //only children that we want to listen to on change are users --- > KeyboardEvent
                if (arg0.getKey() != "AssignUserRole" && arg0.getKey() != "GameStart" && arg0.getKey() != "InfoToClient" && arg0.getKey() != "ScreenNbr" && arg0.getKey() != "NumOfPlayers") {
                    Iterable<DataSnapshot> dsList= arg0.getChildren();
                    Collections.sort(users);
                    int hashedPosition = userNameIDMap.get(arg0.getKey());
                    System.out.println(arg0.getKey()+" "+hashedPosition);

                    for (DataSnapshot dataSnapshot : dsList) {
                        if (dataSnapshot.getKey().equals("KeyboardEvent")){

                            users.get(hashedPosition).replaceWord((String) dataSnapshot.getValue());

                            if (users.get(hashedPosition).getSb().equals(currentWord)) { // if player gets the word correct
                                myFirebaseRef.child("InfoToClient").setValue(hashedPosition); // winners ID is sent to all clients
                                users.get(hashedPosition).updateScore(pointsForWin);
                                pointsForWin -= 3;
                                countWinners += 1;
                                if (countWinners >= 3 || users.size() == countWinners) { // if at least 3 players got the word right or two if there are only two players, new round starts
                                    System.out.println("CountWinners: "+countWinners+" users.Size: "+users.size());
                                    newRound();
                                }


                            }

                        }

                    }
                    repaint();
                }


            }

            @Override
            public synchronized void onChildAdded(DataSnapshot arg0, String arg1) {
                Object valueClient = arg0.getKey();
                if (valueClient != null) {
                    if (arg0.getValue().toString().equals("UserAdded") && !preventPlayersToConnect) {
                        if (newGame) {
                            users.clear();
                            newGame = false;
                        }
                        System.out.println(arg0.getKey()+" "+users.size());
                        userNameIDMap.put(arg0.getKey(), users.size());

                        myFirebaseRef.child(arg0.getKey()+"AssignUserRole").setValue(users.size());
                        myFirebaseRef.child("NumOfPlayers").setValue(users.size());
                        users.add(new User(arg0.getKey().toString(), users.size()));

                    }
                    if (arg0.getValue().toString().equals("GameStart")) {
                        preventPlayersToConnect = true;
                        myFirebaseRef.child("AssignUserRole").setValue(-1);
                        addWord();

                        scrambledWord = scrambleCurrentWord();
                        timerPause.start();// Start the timer here.
                        isGameStarted = true;

                        roundSeconds = 100;
                        pointsForWin = 10;
                        pauseSeconds = 5;
                        countWinners = 0;
                        numOfRounds = 0;
                        gameState = 1;

                    }
                }


                repaint();
            }

            @Override
            public void onCancelled(FirebaseError arg0) {
                // TODO Auto-generated method stub

            }
        });
    }

    //Called when the screen needs a repaint.
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2= (Graphics2D) g;
        g2.setFont(font);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getSize().width, getSize().height);
        g2.setColor(Color.BLACK);

        switch(gameState) {
            case 0: // State 0 - Welcome screen
                g.drawImage(lobbyImage, 0, 0, getWidth(), getHeight(), this);
                //g.drawString("Players:",225,190);
                int offset0 = 50;
                for (User user : users ) {
                    g2.setColor(colors[user.getUniqueID()]);
                    g.setFont(font);
                    g.drawString(user.getId(),325,260+offset0);
                    offset0 += 50;
                }

                break;
            case 1: // State 1 - Starting the game

                g.drawImage(nextRoundImage, 0, 0, getWidth(), getHeight(), this);
                g.setFont(new Font("SegoePrint", Font.PLAIN, 400));
                g.drawString(""+pauseSeconds, 500, 550);
                g.setFont(new Font("SegoePrint", Font.BOLD, 35));
                if (pauseSeconds <= 0) {
                    gameState = 2;
                }

                break;
            case 2:

                // State 2 - game in progress
                g.drawImage(gamePlayImage, 0, 0, getWidth(), getHeight(), this);
                g.setFont(new Font("SegoePrint", Font.BOLD, 72));
                g.drawString(""+scrambledWord, 440, 225);

                g.setFont(new Font("SegoePrint", Font.BOLD, 35));
                g.drawString("Time left: "+roundSeconds, 400, 277);
                g.drawString("Round: "+ (numOfRounds +1), 660, 277);
                int offset1 = 160;
                for (User user : users ) { // first 3 players
                    if (user.getUniqueID() <= 2) {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(""+user.getId(),57,215 + offset1);
                        g.drawString(""+ user.getSb(),57,272 + offset1);
                        g.drawString(""+user.getScore(), 482,  268 + offset1);
                        offset1 += 130;
                    }
                    else if (user.getUniqueID() == 3) // 4th player offset reset
                    {
                        offset1 = 160;
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(""+user.getId(),740,215 + offset1);
                        g.drawString(""+ user.getSb(),740,272 + offset1);
                        g.drawString(""+user.getScore(), 1160,  268 + offset1);
                        offset1 += 130;
                    }
                    else // 5th and 6th player
                    {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(""+user.getId(),740,215 + offset1);
                        g.drawString(""+ user.getSb(),740,272 + offset1);
                        g.drawString(""+user.getScore(), 1160,  268 + offset1);
                        offset1 += 130;
                    }
                }
                break;
            case 3: // State 3 - final results
                Collections.sort(users, new Comparator<User>() {

                    public int compare(User o1, User o2) {
                        if (o1.getScore() < (o2.getScore()))
                            return 1;
                        else if (o1.getScore() > (o2.getScore()))
                            return -1;
                        return 0;
                    }


                });
                g.drawImage(resultsImage, 0, 0, getWidth(), getHeight(), this);
                int counter = 0;
                for (User user : users ) {
                    if (counter == 0) { // winner position
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(user.getId(), 544,  605);
                        //g.drawString(""+user.getScore(), 705,  710);
                    }
                    else if (counter == 1) // 2nd position
                    {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(user.getId(), 230,  604);
                        //g.drawString(""+user.getScore(), 470,  425);
                    }
                    else if (counter == 2) //third position
                    {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(user.getId(), 886,  611);
                        //g.drawString(""+user.getScore(), 940,  415);
                    }
                    else if (counter == 3) //4th position
                    {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(user.getId(), 230,  750);
                        //g.drawString(""+user.getScore(), 940,  415);
                    }
                    else if (counter == 4) //5th position
                    {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(user.getId(), 565,  750);
                        //g.drawString(""+user.getScore(), 940,  415);
                    }
                    else if (counter == 5) //6th position
                    {
                        g2.setColor(colors[user.getUniqueID()]);
                        g.drawString(user.getId(), 894,  750);
                        //g.drawString(""+user.getScore(), 940,  415);
                    }
                    counter += 1;
                }
                break;
        }

    }

    public void addWord() {
        int randomInt = random.nextInt(Constants.scrambleWords.length);
        currentWord = Constants.scrambleWords[randomInt];
    }

    public String scrambleCurrentWord() {

        return shuffle(currentWord);
    }

    public String shuffle(String input){
        List<Character> characters = new ArrayList<Character>();
        for(char c:input.toCharArray()){
            characters.add(c);
        }
        StringBuilder output = new StringBuilder(input.length());
        while(characters.size()!=0){
            int randPicker = (int)(Math.random()*characters.size());
            output.append(characters.remove(randPicker));
        }
        return output.toString();
    }

    public void newRound() {
        if (numOfRounds >= 2) { //After the 3 rounds the game is over
            gameState = 3;
            timerGame.stop();
            timerPause.stop();
            myFirebaseRef.child("InfoToClient").setValue("GameIsFinished"); // keyboard is disabled
            myFirebaseRef.child("InfoToClient").removeValue();
            myFirebaseRef.child("GameStart").removeValue();
            //myFirebaseRef.child("AssignUserRole").removeValue();
            myFirebaseRef.child("InfoClient").removeValue();
            for (User user : users) {
                myFirebaseRef.child(user.getId()).removeValue();
            }
            newGame = true;
            userNameIDMap.clear();
            preventPlayersToConnect = false;

        }else { //After none final round

            addWord();
            scrambledWord = scrambleCurrentWord();
            myFirebaseRef.child("InfoToClient").setValue("NewRound");
            myFirebaseRef.child("InfoToClient").setValue("False"); // keyboard is disabled
            numOfRounds += 1;
            roundSeconds = 100;
            pointsForWin = 10;
            pauseSeconds = 5;
            countWinners = 0;
            timerGame.stop();
            timerPause.start();
            for (User user : users ) {
                user.setSb(new StringBuffer(""));
            }
            gameState = 1;
        }

    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if(ev.getSource()==timerGame){
            //System.out.println(roundSeconds);
            if (isGameStarted) {
                roundSeconds -= 1;

                if (roundSeconds <= 0) {
                    newRound();
                }
            }

        }

        if (ev.getSource() == timerPause) {
            pauseSeconds -= 1;
            //System.out.println(pauseSeconds);
            if (pauseSeconds <= 0) {
                timerPause.stop();
                myFirebaseRef.child("InfoToClient").setValue("True"); //after pause, keyboard is enabled
                timerGame.start();

            }
        }

        repaint();// this will call at every 1 second
    }

}
