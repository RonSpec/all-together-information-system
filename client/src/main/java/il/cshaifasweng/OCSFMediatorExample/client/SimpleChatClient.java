package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import il.cshaifasweng.OCSFMediatorExample.entities.UserMessage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import java.util.List;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.util.Optional;

import static il.cshaifasweng.OCSFMediatorExample.client.ViewTasksController.currentUser;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * JavaFX App
 */
public class SimpleChatClient extends Application {

    private static Scene scene;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static Stage stage;
    private SimpleClient client;

    private static User user = null;


    @Override
    public void start(Stage stage) throws IOException {

        EventBus.getDefault().register(this);
//    	client = SimpleClient.getClient();
//    	client.openConnection();
        scene = new Scene(loadFXML("ConnectToServer"), 500, 500); // TODO: make this modular
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            event.consume();

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("");
            alert.setContentText("Are you sure you want to close the ATIS application?");
            alert.setGraphic(null);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                stage.close();
            }
        });
        stage.show();
    }


    @Subscribe
    public void testEvent(getDataEvent event) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("This is a test");
            alert.setHeaderText("");
            alert.setContentText("Hopefully this is shown to all users");
            alert.showAndWait();
        });
    }

    @Subscribe
    public void NewMessageEvent(NewMessageEvent event) {
        System.out.println(event.getMessage().getObjectsArr().size());
        UserMessage userMessage = (UserMessage) event.getMessage().getObjectsArr().get(0); // Get the usermessage
        String formatted_date = (userMessage.getWas_sent_on()).format(formatter);
        String from = (String) event.getMessage().getObjectsArr().get(1);
        System.out.println(from);

        switch(userMessage.getMessage_type()) { // Switch uses equals()
            case "Community":
            case "Normal": // Create an alert
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("You have a new message");
                    if (userMessage.getMessage_type().equals("Community")) {
                        alert.setHeaderText("From: System, Sent on: " + formatted_date);
                    }
                    else {
                        alert.setHeaderText("From: " + from + ", Sent on: " + formatted_date);
                    }
                    alert.setContentText(userMessage.getMessage());
                    alert.showAndWait();
                });
                break;

            case "Not Complete":
                // Pop up with ok/no
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Pending task completion");
                    alert.setHeaderText("Sent on: " + formatted_date);
                    alert.setContentText(userMessage.getMessage());
                    alert.setGraphic(null);


                    ButtonType buttonTypeYes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                    ButtonType buttonTypeNo = new ButtonType("No", ButtonBar.ButtonData.NO);

                    alert.getButtonTypes().clear();
                    alert.getButtonTypes().addAll(buttonTypeYes,buttonTypeNo);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get().equals(buttonTypeYes)) {
                        // Pop a textinputdialog.
                        TextInputDialog tiDialog = new TextInputDialog();
                        tiDialog.setTitle("Send a completion message to your manager");
                        tiDialog.setHeaderText("Please enter a message to send to your community manager: ");
                        tiDialog.setContentText("Message: ");

                        Optional<String> dialog_result = tiDialog.showAndWait();

                        if (dialog_result.isPresent()) {
                            String to_manager_text = "Task done by: \"" + user.getUserName() + "\"\nHas been marked complete with the message:\n\"" + dialog_result.get() + "\"";
                            UserMessage to_manager_message = new UserMessage(to_manager_text , user.getTeudatZehut(), user.getCommunity().getCommunityManager().getTeudatZehut(), "Normal");
                            Message to_send = new Message("Send message", to_manager_message);

                            try {
                                SimpleClient.getClient().sendToServer(to_send);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    } else  {
                        Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
                        alert2.setTitle("You have a new message");
                        alert2.setHeaderText("");
                        alert2.setContentText("Once you have completed the task, please update your community manager.\nIf you're experiencing trouble completing the task, you may withdraw through the task list.");
                        alert2.showAndWait();
                    }
                });
                break;


        }
    }





    public static void setUser(User user) {
        SimpleChatClient.user = user;
    }

    public static User getUser() {
        return user;
    }




    static void setRoot(String fxml) throws IOException { // TODO: make this modular.
        scene.setRoot(loadFXML(fxml));

    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleChatClient.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    
    

    @Override
    public void stop() throws Exception {
        // TODO Auto-generated method stub
        EventBus.getDefault().unregister(this);
        super.stop();
//        Platform.exit();
//        System.exit(0);
    }

	public static void main(String[] args) {
        launch();
        try {
            System.out.println("closing connection to server");
            SimpleClient client = SimpleClient.getClient();
            client.closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}