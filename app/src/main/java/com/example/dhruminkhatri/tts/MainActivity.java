package com.example.dhruminkhatri.tts;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{


    private final Context context = this;

    private ImageButton btnSpeak;       // Button to fire the speech prompt when it is tapped
    private ImageButton btnTap;         // Button to prompt text input from user when it is tapped
    private LinearLayout llayout;       // Main Layout of the app
    private ScrollView scrollView;      // Main ScrollView
    private int i;
    private final int REQ_CODE_SPEECH_INPUT = 100;      // Required for our text-to-speech part of our application

    private int serverMessage = Gravity.LEFT;
    private int userMessage = Gravity.RIGHT;

    private TextToSpeech tts;          // Text to Speech converter for our app


    private Socket mSocket;            // Our Virtual Socket used for communication with our server

    private class cons {
        boolean newUser;
        boolean newUserFollow;
        boolean questions;
    }

    private cons c;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = (ScrollView)findViewById(R.id.scrollView);     // Finding the ScrollView by ID
        llayout = new LinearLayout(this);                           // Creating a new LinearLayout (this is our root layout and everything we dynamically create will eventually be added to this layout)
        llayout.setOrientation(LinearLayout.VERTICAL);              // Setting the main Layout as vertical

        scrollView.addView(llayout);
        llayout.setPadding(5, 15, 5, 10);                           // Adding padding to the main layout
        btnSpeak = (ImageButton)findViewById(R.id.btnSpeak);        // Adding a Listener to ImageButton so that a speech prompt appears when it is tapped
        btnTap = (ImageButton)findViewById(R.id.btnType);           // Adding a Listener to ImageButton so that a text prompt appears when it is tapped
        tts = new TextToSpeech(this, this);                         // To initialize our Text To Speech Listener


        try {
            mSocket = IO.socket("http://192.168.0.102:8080");
        }catch(URISyntaxException e){
            Log.e("Socket", e.toString());
        }

        {
            c = new cons();
            mSocket.on("message", onMessage);
            mSocket.on("message:newUser", onNewUser);
            mSocket.on("message:newUser:follow", onNewUserSentimentFollow);
            mSocket.on("message:newUser:Yes:follow", onNewUserFollow);
            mSocket.on("message:question", onQuestion);
            mSocket.on("message:question:follow", onQuestion);
            mSocket.connect();
        }

        btnSpeak.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                promptSpeechInput();
            }
        });

        btnTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get prompt.xml view
                LayoutInflater li = LayoutInflater.from(context);
                View promptView = li.inflate(R.layout.prompt, null);

                AlertDialog.Builder aDB = new AlertDialog.Builder(context);
                aDB.setView(promptView);

                final EditText ed = (EditText)promptView.findViewById(R.id.promptEditText);

                aDB.setCancelable(false).setPositiveButton("Send",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendMessage(ed.getText().toString());
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // create a Alert box
                aDB.create();

                // show it
                aDB.show();


            }
        });

    }

    private Emitter.Listener onMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final JSONObject data = (JSONObject)args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    try{
                        message = data.getString("content");
                    }catch(JSONException e){
                        return ;
                    }
                    setResponse(message, serverMessage);
                }
            });
        }
    };

    private Emitter.Listener onNewUser = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final JSONObject data = (JSONObject)args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    try{
                        message = data.getString("content");
                    }catch(JSONException e){
                        return ;
                    }
                    c.newUser = true;
                    setResponse(message, serverMessage);
                }
            });
        }
    };

    private Emitter.Listener onNewUserFollow = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final JSONObject data = (JSONObject)args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    try{
                        message = data.getString("content");
                    }catch (JSONException e){
                        return ;
                    }
                    c.newUserFollow = true;
                    setResponse(message, serverMessage);
                }
            });
        }
    };

    private Emitter.Listener onQuestion = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final JSONObject data = (JSONObject)args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    try{
                        message = data.getString("content");
                    }catch(JSONException e){
                        return ;
                    }
                    c.questions = true;
                    setResponse(message, serverMessage);
                }
            });
        }
    };

    private Emitter.Listener onNewUserSentimentFollow = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final JSONObject data = (JSONObject)args[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message;
                    int scr = 0;
                    try{
                        message = data.getString("content");
                        scr = data.getInt("sentiment");
                    }catch(JSONException e){
                        return ;
                    }
                    if(scr < 0) {
                        c = new cons();
                        mSocket.disconnect();
                        mSocket.connect();
                    }
                    else if(scr > 0) {
                        c.newUser = false;
                        mSocket.emit("message:newUser:Yes");
                    }
                    setResponse(message, serverMessage);
                }
            });
        }
    };


    /*
    Function to send message to the user
     */
    private void sendMessage(String message)
    {
        if(c == null) {
            c = new cons();
            sendMessage(message);
        }
        JSONObject data = new JSONObject();
        try{
            data.put("content", message);
        }catch(JSONException e){
            return ;
        }
        if(c.questions) {
            mSocket.emit("message:question:follow", data);
        }
        else if(c.newUserFollow) {
            Log.i("Here", "Here");
            mSocket.emit("message:newUser:Yes:follow", data);
        }
        else if(!c.newUser) {
            mSocket.emit("message", data);
        }
        else {
            mSocket.emit("message:newUser", data);
        }
        setResponse(message, userMessage);
    }

    /*
    Function to set Responses gathered from either the user or the server
     */
    private void setResponse(String message, int gravity)
    {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setBackground(null);
        t.setText(message);
        t.setTypeface(Typeface.SANS_SERIF);
        t.setTextColor(Color.parseColor("#FFFFFF"));
        /*
        Setting the gravity of the layout as per the caller of the function
        i.e. if the user is calling the function then to the left
            ,and if the server is calling this function then to the right
         */
        t.setGravity(gravity);

        if(gravity == Gravity.RIGHT){
            t.setPadding(0, 10, 20, 0);
        }
        else {
            t.setPadding(20, 10, 0, 0);
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
        t.setTextSize(20);
        llayout.addView(l);
        int width = llayout.getWidth();
        t.setMaxWidth(width / 2);

        l.addView(t);

        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 100L);
    }

    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
            tts.setLanguage(Locale.UK);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }



    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //txtSpeechInput.setText(result.get(0));
                    sendMessage(result.get(0));
                }
                break;
            }

        }
    }
}
