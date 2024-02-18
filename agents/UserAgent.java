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

            Product selectedProduct = window.getSelectedProduct();
            if(!products.contains(selectedProduct)) {
                println("This product is not in my product list");
                return;
            }
            this.productToRepair = selectedProduct;

            boolean isAbleToDetectBreakdown = canDetectBreakdown(selectedProduct);
            if(isAbleToDetectBreakdown) {
                println("I'm able to detect the breakdown -> " + selectedProduct.getDefault().getName() + ", breakdown level : " + selectedProduct.getBreakdownLevel());

                if(selectedProduct.getBreakdownLevel() == 4) {
                    println(selectedProduct.getName() + " is definitely broken");
                    if(this.wallet >= selectedProduct.getPrice()) {
                        this.wallet -= selectedProduct.getPrice();
                        println("I'll bought a new product at distributors for " + selectedProduct.getPrice() + "€");
                    }
                    else println(selectedProduct.getName() +  " I don't have enough money to buy a new one ...");
                    terminateRepair();
                }
                else {
                    println("I'll ask to partStore if they have " + selectedProduct.getDefault().getName());
                    askAvailablePartPrice(selectedProduct);
                }

            }
            else {
                println("I'm not able to detect the breakdown");
                askToRepairCoffeesRendezVousToCheckProduct();
            }

        }
    }

    private void askToRepairCoffeesRendezVousToCheckProduct() {
        var coffees = AgentServicesTools.searchAgents(this, "repair", "coffee");
        for (AID coffee : coffees) {
            sendMessage(coffee,  "is_able_to_repair", ACLMessage.REQUEST);
        }
    }

    public void askAvailablePartPrice(Product requestedProduct) {
        var partsStores = AgentServicesTools.searchAgents(this, "repair", "partstore");
        for(AID partStore : partsStores) {
            sendMessage(partStore, "is_part_available", ACLMessage.REQUEST);
        }

    }
    private void searchRepairOptions() {
        addBehaviour(new Behaviour() {
            private int step = 0;
            private final ACLMessage[] cfpContainer = new ACLMessage[1]; // Conteneur pour la variable cfp
            private List<AID> capableRepairCoffees = new ArrayList<>();

            @Override
            public void action() {
                switch (step) {
                    case 0:
                        cfpContainer[0] = new ACLMessage(ACLMessage.CFP);
                        for (AID repairCoffee : AgentServicesTools.searchAgents(UserAgent.this, "repair", "coffee")) {
                            cfpContainer[0].addReceiver(repairCoffee);
                        }
                        cfpContainer[0].setContent(productToRepair.getType().toString());
                        cfpContainer[0].setConversationId("repair-query");
                        cfpContainer[0].setReplyWith("cfp" + System.currentTimeMillis());
                        myAgent.send(cfpContainer[0]);
                        step = 1;
                        break;
                    case 1:
                        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("repair-query"),
                                MessageTemplate.MatchInReplyTo(cfpContainer[0].getReplyWith()));
                        ACLMessage reply = myAgent.receive(mt);
                        if (reply != null) {
                            if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                capableRepairCoffees.add(reply.getSender());
                            }
                        } else {
                            block();
                        }
                        if (capableRepairCoffees.size() > 0) {
                            step = 2;
                        }
                        break;
                }
            }

            @Override
            public boolean done() {
                if (step == 2) {
                    println("Capable repair coffee options:");
                    for (AID aid : capableRepairCoffees) {
                        println(" - " + aid.getLocalName());
                    }
                    return true;
                }
                return false;
            }
        });
    }


    private void searchPartStores() {
        // Similar logic to searchRepairOptions can be implemented for part stores if required.
    }

    public void println(String s) {
        window.println(s);
    }

    @Override
    public void takeDown() {
        println("Goodbye!");
    }

    public boolean canDetectBreakdown(Product product) {
        if(this.skill == 0) return false;
        else return product.getBreakdownLevel() <= this.skill;
    }



    public void sendMessage(AID recipient, String content, int messageType) {
        ACLMessage message = new ACLMessage(messageType);
        message.setContent(content);
        message.addReceiver(recipient);
        send(message);
    }

    public void terminateRepair(){
        Product productTerminated = this.productToRepair;
        println("-".repeat(10) + " product process terminated " + "-".repeat(10));
        println("Current wallet : " + this.wallet + " €");
    }

}
