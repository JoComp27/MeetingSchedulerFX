/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.ClientGUIController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author GamingPC
 */
public class ServerGUIController implements Initializable {

    private Server server;

    @FXML
    private Label label;

    @FXML
    private TextArea outputLogTextarea;
    
    @FXML
    private void handleButtonAction(ActionEvent event) {
        System.out.println("You clicked me!");
        label.setText("Hello World!");
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    public void initializeServer(){
        this.server = new Server();
        Thread thread = new Thread(server);
        thread.start();

        Thread autoRefreshThread = new Thread(new AutoRefresh());
        autoRefreshThread.start();

    }

    private void refreshElements() {

        //Get Outputs from client
        updateOutputTextArea(server.getServerLog());

    }

    private void updateOutputTextArea(List<String> outputMessages){

        Platform.runLater(() -> {

            String msg = "";

            for(String submsg : outputMessages){
                msg += submsg + "\n";
            }

            outputLogTextarea.setText(msg);
        });

    }

    public void loadServer() {
        server.loadServer();
    }

    private class AutoRefresh implements Runnable{

        @Override
        public void run() {

            while(true){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                refreshElements();
            }

        }
    }
    
}
