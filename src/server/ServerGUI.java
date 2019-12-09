/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.Scanner;

/**
 *
 * @author GamingPC
 */
public class ServerGUI extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ServerGUI.fxml"));

        Parent root = (Parent)fxmlLoader.load();

        ServerGUIController controller = fxmlLoader.<ServerGUIController>getController();

        controller.initializeServer();

        //Checking if previous
        File saveFile = new File("server.txt");

        if(saveFile.exists()){

            //Add CLI to check if user wants to restore user or not.

            System.out.println("It seems that a restore file is available and could be loaded onto the" +
                    "server\n Do you wish to restore it?");

            String answer = "";

            Scanner scanner = new Scanner(System.in);

            while(!answer.equals("y") && !answer.equals("n")){

                answer = scanner.nextLine().trim();

                switch (answer) {
                    case "y":
                        System.out.println("Save will be restored for server");
                        controller.loadServer();
                        break;
                    case "n":
                        System.out.println("Save will not be restored for client");
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
