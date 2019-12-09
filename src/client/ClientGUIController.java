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
import java.util.ArrayList;
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
    private RadioButton RequestRadioButton;
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

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        outputMessages = new ArrayList<>();
        availableMeetingNumber = new ArrayList<>();

        requestTypesToggleGroup = new ToggleGroup();
        RequestRadioButton.setToggleGroup(requestTypesToggleGroup);
        widthdrawRadioButton.setToggleGroup(requestTypesToggleGroup);
        addRadioButton.setToggleGroup(requestTypesToggleGroup);
        requesterCancel.setToggleGroup(requestTypesToggleGroup);

        RequestRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                currentlySelectedRequest = RequestType.Request;
                refreshElements();
            }
        });

        widthdrawRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                currentlySelectedRequest = RequestType.Withdraw;
                refreshElements();
            }
        });

        addRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                currentlySelectedRequest = RequestType.Add;
                refreshElements();
            }
        });


        requesterCancel.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                currentlySelectedRequest = RequestType.RequesterCancel;
                refreshElements();
            }
        });


    }

    public void initializeClient(String name) {

        this.client = new Client(name);
        Thread thread = new Thread(client);
        thread.start();

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

    private void updateOutputTextArea(ArrayList<String> outputMessages){

        Platform.runLater(() -> {

            String msg = "";

            for(String submsg : outputMessages){
                msg += submsg + "\n";
            }

            outputLogTextarea.setText(msg);
        });

    }

    private void updateMeetingNumberComboBox(){

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

}
