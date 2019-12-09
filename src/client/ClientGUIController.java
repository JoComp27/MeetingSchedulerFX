/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import requests.RequestType;

import java.net.URL;
import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author GamingPC
 */
public class ClientGUIController implements Initializable {

    private Client client;

    private ArrayList<Integer> availableMeetingNumber;
    private ArrayList<String> outputMessages;

    private RequestType currentlySelectedRequest;

    private ToggleGroup requestTypesToggleGroup;

    @FXML
    private Label clientNameLabel;

    @FXML
    private RadioButton requestRadioButton;
    @FXML
    private RadioButton widthdrawRadioButton;
    @FXML
    private RadioButton addRadioButton;
    @FXML
    private RadioButton requesterCancel;

    @FXML
    private ComboBox<Integer> meetingNumberComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField timeTextField;
    @FXML
    private TextField minimumTextField;
    @FXML
    private TextField participantsTextField;
    @FXML
    private TextField topicTextField;

    @FXML
    private Button sendButton;

    @FXML
    private TextArea outputLogTextarea;

    @FXML
    private void handleSendButtonAction(ActionEvent event) {

        if(currentlySelectedRequest.equals(RequestType.Request)){

            LocalDate date = datePicker.getValue();
            String time = timeTextField.getText();
            String minimum = minimumTextField.getText();
            String participants = participantsTextField.getText();
            String topic = topicTextField.getText();

            if(date == null || time == null || minimum == null || participants == null){
                System.out.println("One or more field is empty");
                return;
            }

            int timeInt = 0;
            int min = 0;

            try{
                timeInt = Integer.parseInt(time);
                min = Integer.parseInt(minimum);
            } catch(NumberFormatException e) {
                System.out.println("INVALID TIME OR MINIMUM VALUES");
                return;
            }

            List<String> participantsList = new ArrayList<>();
            String[] participantsArray = participants.split(",");

            if(min < participantsArray.length){
                System.out.println("MIN IS LARGER THAN THE NUMBER OF INVITEES");
                return;
            }

            for(String element : participantsArray){
                participantsList.add(element.trim());
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), timeInt, 0, 0);

            client.sendRequest(calendar, min, participantsList, topic);
        }


        else if(currentlySelectedRequest.equals(RequestType.Add)){

            Integer meetingNumber = meetingNumberComboBox.getSelectionModel().getSelectedItem();

            if(meetingNumber != null){
                client.sendAdd(meetingNumber);
            }

        }
        else if(currentlySelectedRequest.equals(RequestType.Withdraw)){

            Integer meetingNumber = meetingNumberComboBox.getSelectionModel().getSelectedItem();

            if(meetingNumber != null){
                client.sendWithdraw(meetingNumber);
            }

        }
        else if(currentlySelectedRequest.equals(RequestType.RequesterCancel)){

            Integer meetingNumber = meetingNumberComboBox.getSelectionModel().getSelectedItem();

            if(meetingNumber != null){
                client.sendRequesterCancel(meetingNumber);
            }

        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        outputMessages = new ArrayList<>();
        availableMeetingNumber = new ArrayList<>();

        requestTypesToggleGroup = new ToggleGroup();
        requestRadioButton.setToggleGroup(requestTypesToggleGroup);
        widthdrawRadioButton.setToggleGroup(requestTypesToggleGroup);
        addRadioButton.setToggleGroup(requestTypesToggleGroup);
        requesterCancel.setToggleGroup(requestTypesToggleGroup);

        requestRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                currentlySelectedRequest = RequestType.Request;
                refreshElements();
            }
        });

        widthdrawRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                currentlySelectedRequest = RequestType.Withdraw;
                refreshElements();
            }
        });

        addRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                currentlySelectedRequest = RequestType.Add;
                refreshElements();
            }
        });


        requesterCancel.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                currentlySelectedRequest = RequestType.RequesterCancel;
                refreshElements();
            }
        });

        Platform.runLater(() -> {
            meetingNumberComboBox.setDisable(true);
            datePicker.setDisable(true);
            timeTextField.setDisable(true);
            minimumTextField.setDisable(true);
            participantsTextField.setDisable(true);
            topicTextField.setDisable(true);
            sendButton.setDisable(true);
        });

    }

    public void initializeClient(String name) {

        this.client = new Client(name);
        Thread thread = new Thread(client);
        thread.start();

        Platform.runLater(() -> {
            clientNameLabel.setText("Client Name: " + name);
        });

        Thread autoRefreshThread = new Thread(new AutoRefresh());
        autoRefreshThread.start();

    }

    public void restoreFromSave(String fileName){
        client.restoreFromSave(fileName);
    }


    private void refreshElements() {

        //Get Outputs from client
        updateOutputTextArea(client.getLog());

        //Update Meeting Number list
        updateMeetingNumberComboBox();

        //Change the available input fields
        updateAvailableInputFields();


    }

    private void updateAvailableInputFields() {

        Platform.runLater(() -> {

            if(currentlySelectedRequest == null){

                meetingNumberComboBox.setDisable(true);
                datePicker.setDisable(true);
                timeTextField.setDisable(true);
                minimumTextField.setDisable(true);
                participantsTextField.setDisable(true);
                topicTextField.setDisable(true);
                sendButton.setDisable(true);

                return;
            }

            boolean isRequest = false;

            if(currentlySelectedRequest.equals(RequestType.Request)){
                isRequest = true;
            }

            meetingNumberComboBox.setDisable(isRequest);
            datePicker.setDisable(!isRequest);
            timeTextField.setDisable(!isRequest);
            minimumTextField.setDisable(!isRequest);
            participantsTextField.setDisable(!isRequest);
            topicTextField.setDisable(!isRequest);

        });

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

    private void updateMeetingNumberComboBox(){

        if(currentlySelectedRequest == null){
            return;
        }

        ArrayList<Integer> meetingNumbers;

        if(currentlySelectedRequest.equals(RequestType.Withdraw)) {
            //Fill in meeting numbers that are complete and that the client has said yes to
            meetingNumbers = client.getWidthdrawNumbers();

        }
        else if(currentlySelectedRequest.equals(RequestType.Add)) {
            //Fill in meeting numbers that the client has said no to and that the date would be available now.

            meetingNumbers = client.getAddNumbers();

        }
        else if(currentlySelectedRequest.equals(RequestType.RequesterCancel)) {
            //Fill in meeting numbers that client is a requester of

            meetingNumbers = client.getRequesterNumbers();

        } else {
            return;
        }

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
