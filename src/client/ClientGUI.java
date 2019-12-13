/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.Scanner;

/**
 * @author GamingPC
 */
public class ClientGUI extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientGUI.fxml"));

        Parent root = (Parent) fxmlLoader.load();

        ClientGUIController controller = fxmlLoader.<ClientGUIController>getController();

        String clientName = getParameters().getRaw().get(0);

        String addressPort = getParameters().getRaw().get(1);

        String fileName = "saveFile_" + clientName;

        //Checking if previous
        File saveFile = new File(fileName + ".txt");

        if (saveFile.exists()) {

            //Add CLI to check if user wants to restore user or not.

            System.out.println("It seems that a restore file is available and could be loaded onto the" +
                    "client\nDo you wish to restore it?");

            String answer = "";

            Scanner scanner = new Scanner(System.in);

            while (!answer.equals("y") && !answer.equals("n")) {

                answer = scanner.nextLine().trim();

                switch (answer) {
                    case "y":
                        System.out.println("Save will be restored for client " + clientName);
                        controller.initializeClient(clientName, addressPort);
                        controller.restoreFromSave(fileName);
                        break;
                    case "n":
                        System.out.println("Save will not be restored for client");
                        controller.initializeClient(clientName, addressPort);
                        break;
                    default:
                        System.out.println("INVALID SAVE RESTORE ANSWER");
                }

            }


        }

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }


}
