package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.data.Product;
import handsOn.circularEconomy.data.ProductType;
import handsOn.circularEconomy.gui.UserAgentWindow;
import jade.core.AID;
import jade.core.AgentServicesTools;
import jade.core.behaviours.Behaviour;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UserAgent extends GuiAgent {
    private List<Product> products;
    private int skill;
    private UserAgentWindow window;
    private Product productToRepair;
    private double wallet = 0;

    @Override
    public void setup() {
        this.window = new UserAgentWindow(getLocalName(),this);
        window.setButtonActivated(true);
        //add a random skill
        Random hasard = new Random();
        skill = hasard.nextInt(5);
        Random random = new Random();
        wallet = 250.0 + (500.0 - 250.0) * random.nextDouble();
        println("hello, I have a skill = "+ skill);
        println("My wallet amount is " + new DecimalFormat("#.##").format(wallet) + "€");
        //add some products choosen randomly in the list Product.getListProducts()
        products = new ArrayList<>();
        int nbTypeOfProducts = ProductType.values().length;
        int nbPoductsByType = Product.NB_PRODS / nbTypeOfProducts;
        var existingProducts = Product.getListProducts();
        //add products
        for(int i=0; i<nbTypeOfProducts; i++)
            if(hasard.nextBoolean())
                try {
                    products.add(existingProducts.get(hasard.nextInt(nbPoductsByType) + (i*nbPoductsByType)));
                } catch (IndexOutOfBoundsException ignored) { }
        //we need at least one product
        while (products.isEmpty()) {
            try {
                products.add(existingProducts.get(hasard.nextInt(nbPoductsByType*nbTypeOfProducts)));
            } catch (IndexOutOfBoundsException ignored) { }
        }
        window.addProductsToCombo(products);
        println("Here are my objects : ");
        products.forEach(p->println("\t"+p));

    }

    @Override
    public void onGuiEvent(GuiEvent evt)
    {
        //if it is the OK button
        if(evt.getType()==UserAgentWindow.OK_EVENT)
        {
            //search about repair coffee
            var coffees = AgentServicesTools.searchAgents(this, "repair", "coffee");
            println("-".repeat(30));
            for(AID aid:coffees)
                println("found this repair coffee : " + aid.getLocalName());
            println("-".repeat(30));


        }
    }

    public void println(String s) {
        window.println(s);
    }

    @Override
    public void takeDown() {
        println("Goodbye!");
    }
}
