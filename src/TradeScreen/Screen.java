package TradeScreen;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.String;

import OrderManager.Order;

public class Screen extends Application implements TradeScreen
{
    Button button;
    //Tab tabs;

    public void mainScreen(){
        launch();
    }

    public void start(Stage primaryStage) {
        setUserAgentStylesheet(STYLESHEET_MODENA);

        TabPane tabs = new TabPane();

        Tab orderNew = new Tab();
        orderNew.setText("New Order");
        orderNew.setContent(newOrderButton());

        Tab orderAccept = new Tab();
        orderAccept.setText("Accept Order");
        orderAccept.setContent(acceptOrder());

        Tab slice = new Tab();
        slice.setText("Slice");
        slice.setContent(slice());

        Tab price = new Tab();
        price.setText("Price");
        price.setContent(price());

        tabs.getTabs().addAll(orderNew, orderAccept, slice, price);

        //StackPane layout = new StackPane();
        //layout.getChildren().add(button);
        Scene scene = new Scene(tabs, 510, 401);
        primaryStage.setTitle("Trader Screen");
        //tab = new Tab("New Order");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public Button newOrderButton() {

        NewOrderHandler newOrderHandler = new NewOrderHandler();
        button = new Button("New Order");
        button.setOnAction(e -> newOrderHandler.setup(id, order));
        Text textArea = new Text();
        textArea.setText("ID: " + id + "Order: " + order);
        return button;
    }

    public Button newOrder(int id, Order order) {

        NewOrderHandler newOrderHandler = new NewOrderHandler();
        button = new Button("New Order");
        button.setOnAction(e -> newOrderHandler.setup(id, order));
        Text textArea = new Text();
        textArea.setText("ID: " + id + "Order: " + order);
        return button;
    }

    @Override
    public void acceptOrder(int id) throws IOException {

    }

    @Override
    public void sliceOrder(int id, int sliceSize) throws IOException {

    }

    @Override
    public void price(int id, Order o) throws InterruptedException, IOException {

    }

    private Button acceptOrder() {

        button = new Button("Accept Order");
        button.setOnAction(e -> System.out.println("Accepted"));

        return button;
    }

    private Button slice() {
        button = new Button("Slice");
        button.setOnAction(e -> System.out.println("Sliced"));
        return button;

    }
    private Button price() {
        button = new Button("Price");
        button.setOnAction(e -> System.out.println("Priced"));

        return button;
    }


}


