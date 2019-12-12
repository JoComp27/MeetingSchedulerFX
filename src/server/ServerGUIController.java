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
import javafx.scene.control.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author GamingPC
 */
public class ServerGUIController implements Initializable {

    private Server server;

    @FXML
    private ComboBox<Integer> meetingNumberComboBox;
    @FXML
    private TextField newRoomTextField;
    @FXML
    private Button sendButton;

    @FXML
    private TextArea outputLogTextarea;

    @FXML
    private void handleButtonAction(ActionEvent event) {

        if(meetingNumberComboBox.getSelectionModel().getSelectedItem() == null ||
            newRoomTextField.getText() == null){
            return;
        }

        Integer meetingNumber = meetingNumberComboBox.getSelectionModel().getSelectedItem();
        Integer newRoomNumber = Integer.parseInt(newRoomTextField.getText());

        server.sendRoomChangeMessage(meetingNumber, newRoomNumber);

    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        meetingNumberComboBox.onMouseClickedProperty().addListener((observable, oldValue, newValue) -> {
            if(meetingNumberComboBox.getSelectionModel().getSelectedItem() != null && newRoomTextField.getText() != null){
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                });
            } else {
                Platform.runLater(() -> {
                    sendButton.setDisable(true);
                });
            }
        });

        newRoomTextField.onInputMethodTextChangedProperty().addListener((observable, oldValue, newValue) -> {
            if(meetingNumberComboBox.getSelectionModel().getSelectedItem() != null && newRoomTextField.getText() != null){
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                });
            } else {
                Platform.runLater(() -> {
                    sendButton.setDisable(true);
                });
            }
        });

        Platform.runLater(() -> {
            sendButton.setDisable(true);
        });

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

        //Update Meeting Number list
        updateMeetingNumberComboBox();

    }

    private void updateMeetingNumberComboBox(){

        List<Integer> meetingNumbers = server.getMeetingNumbers();

        if(meetingNumberComboBox.getItems().isEmpty()){
            meetingNumberComboBox.getItems().setAll(meetingNumbers);
        } else {

            if(meetingNumberComboBox.getItems().equals(meetingNumbers)){
                return;
            }

            Integer selectedItem = meetingNumberComboBox.getSelectionModel().getSelectedItem();
            if(selectedItem != null && meetingNumbers.contains(selectedItem)){

                Platform.runLater(() -> {
                    meetingNumberComboBox.getItems().setAll(meetingNumbers);
                    meetingNumberComboBox.getSelectionModel().select(selectedItem);
                });

            } else {

                Platform.runLater(() -> {
                    meetingNumberComboBox.getItems().setAll(meetingNumbers);
                });

            }

        }

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
