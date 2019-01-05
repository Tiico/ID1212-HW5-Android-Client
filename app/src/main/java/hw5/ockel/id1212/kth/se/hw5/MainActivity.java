package hw5.ockel.id1212.kth.se.hw5;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;

import hw5.ockel.id1212.kth.se.hw5.model.GameActionFeedback;
import hw5.ockel.id1212.kth.se.hw5.net.Callback;
import hw5.ockel.id1212.kth.se.hw5.net.ServerHandler;

public class MainActivity extends AppCompatActivity implements Callback  {
    public static final String EXTRA_MESSAGE = "com.example.hangman.MESSAGE";
    public static final String EXTRA_TOSTART = "com.example.hangman.TOSTART";
    ServerHandler server = new ServerHandler(this);

    TextView progress, remainingGuesses, score, finishedWord, guessed = null;
    Button quitButton, startButton, guessButton = null;
    EditText guessInput = null;

    private Boolean gameOngoing = false;
    private Boolean connectedToServer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hangman);

        new ConnectServer().execute();

        myApp app = ((myApp)getApplication());
        app.setServerHandler(server);

        progress = (TextView)findViewById(R.id.notify_text);
        remainingGuesses = (TextView)findViewById(R.id.remainingGuesses);
        score = (TextView)findViewById(R.id.currentScore);
        finishedWord = (TextView)findViewById(R.id.finishedWord);
        guessed = (TextView)findViewById(R.id.alreadyGuessedChars);

        quitButton = (Button)findViewById(R.id.sendQuitMessage);
        startButton = (Button)findViewById(R.id.sendStartMessage);
        guessButton = (Button)findViewById(R.id.sendGuess);

        guessInput = (EditText)findViewById(R.id.guessData);
        guessButton.setEnabled(false);

        guessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                server.makeGuess(guessInput.getText().toString());
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishedWord.setText(null);
                if (gameOngoing) {
                    server.restartGame();
                } else {
                    server.startGame();
                }
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectedToServer)
                    server.quitGame();
                else {
                    new ConnectServer().execute();
                }
            }
        });

    }

    private void clearUI(final boolean fullClear) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (fullClear) {
                    remainingGuesses.setText(R.string.remainingGuessesTextView);
                    score.setText(R.string.scoreTextView);
                    progress.setText(null);
                }
                finishedWord.setText(null);
                guessed.setText(null);
            }
        });
    }

    @Override
    public void messageSent() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                guessInput.setText(null);
            }
        });
    }

    @Override
    public void messageReceived(String message) {
        if (!message.contains("|"))
            informAction(message);
        else {
            class UIRunner {
                String[] params;

                private UIRunner(String[] params) {
                    this.params = params;
                }

                void updateUI() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setText(params[1].trim());
                            if (!params[2].trim().equals("_")) {
                                guessed.setText(getString(R.string.wrongGuessesTextView) + " " + params[2]);
                            }
                            remainingGuesses.setText(getString(R.string.remainingGuessesTextView) + " " + params[3]);
                            score.setText(getString(R.string.scoreTextView) + " " + params[4]);
                            if (params.length > 5) {
                                finishedWord.setText(getString(R.string.secretReveal) + " " + params[5]);
                            }
                        }
                    });
                }
            }
            String[] params = message.split("\\|");
            new UIRunner(params).updateUI();
        }
    }

    private void setGameOngoing(boolean ongoing) {
        gameOngoing = ongoing;
        if (gameOngoing) {
            startButton.setText(R.string.sendRestartMessage);
            guessButton.setEnabled(true);
        } else {
            startButton.setText(R.string.sendStartMessage);
            guessButton.setEnabled(false);
        }
    }

    private class ConnectServer extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... _) {
            try {
                server.connect("10.0.2.2", 54321);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void informAction(String message) {
        class UIRunner {
            private String message;

            private UIRunner(String message) {
                this.message = message;
            }

            private void displayMessage() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String toastMsg = "Unknown message";
                        GameActionFeedback gaf = GameActionFeedback.valueOf(message);
                        switch (gaf) {
                            case DUPLICATE_GUESS:
                                toastMsg = "You already guessed this";
                                break;
                            case NO_GAME_STARTED:
                                toastMsg = "No game started";
                                break;
                            case GAME_WON:
                                setGameOngoing(false);
                                toastMsg = "YOU WON!";
                                break;
                            case GAME_LOST:
                                setGameOngoing(false);
                                toastMsg = "YOU LOST";
                                break;
                            case GAME_QUIT:
                                toastMsg = "Quit game";
                                clearUI(true);
                                break;
                            case GAME_ONGOING:
                                toastMsg = "A game is already ongoing";
                                break;
                            case GAME_STARTED:
                                setGameOngoing(true);
                                toastMsg = "Game started";
                                clearUI(false);
                                setGameOngoing(true);
                                break;
                            case GAME_RESTARTED:
                                toastMsg = "Game restarted";
                                clearUI(false);
                                setGameOngoing(true);
                                break;
                            case INVALID_COMMAND:
                                toastMsg = "Invalid command";
                                break;
                            case HELP:
                            case GAME_INFO:
                                return;
                        }
                        Toast.makeText(MainActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        new UIRunner(message).displayMessage();
    }

    @Override
    public void notifyConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectedToServer = true;
                quitButton.setText(R.string.sendQuitMessage);
                guessButton.setEnabled(true);
                startButton.setEnabled(true);
                Toast.makeText(MainActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void notifyDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectedToServer = false;
                guessButton.setEnabled(false);
                startButton.setEnabled(false);
                quitButton.setText(R.string.sendReconnectMessage);
                Toast.makeText(MainActivity.this, "Disconnected from server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}