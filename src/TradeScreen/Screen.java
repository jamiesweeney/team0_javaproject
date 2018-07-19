package TradeScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.String;
import java.net.Socket;
import java.util.ArrayList;
import OrderManager.Order;

public class Screen extends Application implements Runnable
{
    static String name;
    static int port;
    private static Socket omConn;

    Text text;
    Button buttonorder1;
    Button buttonorder2;
    Button buttonorder3;
    Button buttonorder4;
    Button buttonorder5;
    Label statusLabel;
    Button button;
    Button button2;

    ObjectInputStream is;

    ObjectOutputStream os;

    TraderLogic trader;
    ArrayList<Order> orders = new ArrayList<>();
    ArrayList<Button> buttons = new ArrayList<>();

    // Do not remove this
    public Screen () {}

    public Screen (String name,int port) {
        this.name = name;
        this.port = port;
    }

    public void run (){
        launch();
    }

    @Override
    public void start(Stage primaryStage){

        statusLabel = new Label();
        text =new Text("Order Information");

        buttonorder1 = new Button("Empty");
        buttonorder2 = new Button("Empty");
        buttonorder3 = new Button("Empty");
        buttonorder4 = new Button("Empty");
        buttonorder5 = new Button("Empty");
        buttons.add(buttonorder1);
        buttons.add(buttonorder2);
        buttons.add(buttonorder3);
        buttons.add(buttonorder4);
        buttons.add(buttonorder5);

        button = new Button("Accept");
        button2 = new Button("Slice");

        // Grid Pane stuff
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(0, 0, 0, 0));
        gridPane.setHgap(10);
        gridPane.setVgap(30);
        gridPane.add(text, 3,1);
        gridPane.add(buttonorder1, 10,0,6,2);
        gridPane.add(buttonorder2, 10,1,6,2);
        gridPane.add(buttonorder3, 10,2,6,2);
        gridPane.add(buttonorder4, 10,3,6,2);
        gridPane.add(buttonorder5, 10,4,6,2);
        gridPane.add(button, 1,4,3,1);
        gridPane.add(button2, 4,4,3,1);
        gridPane.add(statusLabel, 0, 0,2, 1);

        Scene scene = new Scene(gridPane, 400, 300);
        primaryStage.setTitle("Trader Screen");
        primaryStage.setScene(scene);
        primaryStage.show();
        startTask();
    }

    public void startTask() {
        Runnable task = () -> {
            trader = new TraderLogic(name, port);
            trader.setScreen(this);
            Thread backgroundThread = new Thread(trader);
            backgroundThread.setDaemon(true);
            backgroundThread.start();
            //runTask();
        };
        Thread backgroundThread = new Thread(task);
        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }
//    public void runTask() {
//        for (int i = 1; i <= 5; i++) {
//            final String status = "Hello " + name;
//            Platform.runLater(() -> {
//                statusLabel.setText(status);
//                button.setOnAction(event -> text.setText("Button Pressed"));
//            });
//        }
//    }

    private void updateButtons(Order order) {
        int index;
        if ((index = orders.indexOf(order)) < 5) {
            System.out.println(index);
            Button button = buttons.get(index);
            Platform.runLater(() -> {
                button.setText("" + order.id);
            });
        }
    }

    public void addOrder(Order order) {
        orders.add(order);
        updateButtons(order);
    }
}
