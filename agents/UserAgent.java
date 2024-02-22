package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.ACLMessagesObject.PartAvailableMessage;
import handsOn.circularEconomy.ACLMessagesObject.PartWithStore;
import handsOn.circularEconomy.data.BreakdownLevel;
import handsOn.circularEconomy.data.Part;
import handsOn.circularEconomy.data.Product;
import handsOn.circularEconomy.data.ProductType;
import handsOn.circularEconomy.gui.UserAgentWindow;
import jade.core.AID;
import jade.core.AgentServicesTools;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserAgent extends GuiAgent {
    private List<Product> products;
    private int skill;
    private UserAgentWindow window;
    private Product productToRepair;
    private double wallet = 0;
    private Map<AID, LocalDate> appointments;

    private List<PartWithStore> partWithStore;

    private int numberOfRepairCoffeeAvailable;
    private int numberOfRepairCoffeeUnavailable;
    private int check;
    private int noStoreCount;

    private int coffeeCount;
    private int partStoreNumber;



    @Override
    public void setup() {
        this.window = new UserAgentWindow(getLocalName(),this);
        numberOfRepairCoffeeAvailable = 0;
        check = 0;
        numberOfRepairCoffeeUnavailable = 0;
        appointments = new HashMap<>();
        noStoreCount = 0;
        partStoreNumber = 4;
        partWithStore = new ArrayList<>();
        coffeeCount = AgentServicesTools.searchAgents(this, "repair", "coffee").length;



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

        addBehaviour(new CyclicBehaviour(this){
            public void action(){
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (message != null){
                    switch (message.getConversationId()){
                        case "je peux aider":
                            println(message.getContent() + " est disponible");
                            println("-".repeat(30));
                            numberOfRepairCoffeeAvailable+= 1;
                            break;
                        case "je ne peux pas aider":
                            numberOfRepairCoffeeUnavailable += 1;
                            if (numberOfRepairCoffeeUnavailable == coffeeCount){
                                println("aucun café n'a la spécialité");
                                numberOfRepairCoffeeUnavailable = 0;
                            }
                            break;
                        case "j'ai la piece":
                            Part part = fromString(message.getContent());
                            var sender = message.getSender();
                            println(message.getSender().getLocalName() + " possède la pièce et coûte " + part.getStandardPrice() + "€");
                            println("-".repeat(30));
                            check += 1;
                            PartWithStore partWithStore1 = new PartWithStore(sender, part);
                            partWithStore.add(partWithStore1);

                            if (check == partStoreNumber){
                                buyPart(getStoreWithBestPriceForPart(partWithStore));
                                partWithStore = new ArrayList<>();
                                check= 0;
                                noStoreCount = 0;
                                terminateRepair();

                            }
                            break;
                        case "je n'ai pas la piece":
                            check += 1;
                            noStoreCount += 1;
                            if (check == partStoreNumber && !partWithStore.isEmpty()){
                                buyPart(getStoreWithBestPriceForPart(partWithStore));
                                partWithStore = new ArrayList<>();
                                check =0;
                                noStoreCount = 0;
                                terminateRepair();

                            }
                            if (noStoreCount == partStoreNumber){
                                println("aucun magasin n'a la pièce");
                                noStoreCount = 0;
                                check = 0;
                                noMoreChoice();
                            }
                            break;
                        case "proposition de date":
                            check += 1;
                            println(message.getSender().getLocalName() + " propose ce rdv : ");
                            LocalDate rdv = LocalDate.parse(message.getContent());
                            appointments.put(message.getSender(), rdv);
                            println(rdv.toString());
                            if (check == numberOfRepairCoffeeAvailable){
                                try {
                                    acceptAppointment(findAIDWithMostRecentDate(appointments));
                                    appointments = new HashMap<>();
                                    check = 0;
                                    numberOfRepairCoffeeAvailable = 0;
                                    numberOfRepairCoffeeUnavailable = 0;

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            break;
                        case "j'achete la piece":
                            println("j'achète");
                            Part partToBuy = fromString(message.getContent());
                            withdrawMoney(partToBuy.getStandardPrice());
                            givePart(message);
                            break;
                        default:
                        case "objet repare":
                            println("merci");
                            terminateRepair();
                            break;
                        case "objet foutu", "aucun store disponible":
                           noMoreChoice();
                           break;

                    }
                }

            }
        });

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
           /* for(AID aid:coffees)
                println("found this repair coffee : " + aid.getLocalName());
            println("-".repeat(30));*/

            Product selectedProduct = window.getSelectedProduct();
            if(!products.contains(selectedProduct)) {
                println("This product is not in my product list");
                return;
            }
            this.productToRepair = selectedProduct;


            boolean isAbleToDetectBreakdown = isAbleToDetectBreakdown(selectedProduct);
            if(isAbleToDetectBreakdown) {
                println("Je suis capable de repérer le problème de -> " + selectedProduct.getDefault().getName() + ", qui est de niveau : " + selectedProduct.getBreakdownLevel());

                if(selectedProduct.getBreakdownLevel() == BreakdownLevel.DEFINITIVE.getLevel()) {
                    println(selectedProduct.getName() + " il est foutu de chez foutu");
                    if(this.wallet >= selectedProduct.getPrice()) {
                        this.wallet -= selectedProduct.getPrice();
                        println("J'achèterai un nouveau produit auprès des distributeurs pour la modique somme de  " + selectedProduct.getPrice() + "€");
                    }
                    else println(selectedProduct.getName() +  " Je suis pauvre, je ne peux pas acheter de nouveau produit ... ;( ");
                    terminateRepair();
                }
                else {
                    println("Je vais demander au magasin s'ils ont des pièces pour " + selectedProduct.getDefault().getName());
                    try {
                        askPartPrice(selectedProduct);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
            else {
                println("Je ne peux pas détecter de problème car je suis mauvais");
                try {
                    askToRepairCoffeesRendezVousToCheckProduct();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }


        }
    }

    private void askToRepairCoffeesRendezVousToCheckProduct() throws IOException {
        var coffees = AgentServicesTools.searchAgents(this, "repair", "coffee");
        for (AID coffee : coffees) {
            ACLMessage informMessage = new ACLMessage(ACLMessage.REQUEST);
            informMessage.addReceiver(coffee);
            informMessage.setConversationId("puis-je avoir de l'aide ?");
            informMessage.setContentObject(this.productToRepair);
            send(informMessage);
        }
    }

    private void askPartPrice(Product selectedProduct) throws IOException {
        var partsStores = AgentServicesTools.searchAgents(this, "repair", "partstore");
        for (AID partStore : partsStores){
            ACLMessage informMessage = new ACLMessage(ACLMessage.REQUEST);
            informMessage.addReceiver(partStore);
            informMessage.setConversationId("est-ce que y'a des pieces pour cet objet ?");
            try {
                informMessage.setContent(selectedProduct.getDefault().getName());
            }catch (Exception e){}
            send(informMessage);
        }
    }

    void terminateRepair(){
        Product productTerminated = this.productToRepair;
        this.products.remove(productTerminated);
        println("-".repeat(10) + " product process terminated " + "-".repeat(10));
        println("Argent actuel : " + this.wallet + " €");
        window.refreshProductList(products);
    }

    public boolean isAbleToDetectBreakdown(Product product) {
        if(this.skill == 0) return false;
        else return product.getBreakdownLevel() <= this.skill;
    }
    public void println(String s) {
        window.println(s);
    }

    @Override
    public void takeDown() {
        println("Goodbye!");
    }


    public static Part fromString(String partString) {
        // Regular expression to match the part format
        String regex = "Part\\{ ([^\\-]+\\-part\\d+) \\- ([^\\-]+) \\- ([\\d,]+)€\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(partString);

        if (matcher.find()) {
            String name = matcher.group(1);
            String typeName = matcher.group(2);
            double price = Double.parseDouble(matcher.group(3).replace(",", "."));

            // Find the corresponding ProductType
            ProductType type = ProductType.valueOf(typeName);

            // Create a new Part object with the extracted information
            return new Part(name, type, price);
        } else {
            throw new IllegalArgumentException("String format is incorrect: " + partString);
        }
    }


    public AID findAIDWithMostRecentDate(Map<AID, LocalDate> aidDates) {
        if (aidDates == null || aidDates.isEmpty()) {
            return null; // ou lever une exception selon le besoin
        }

        Map.Entry<AID, LocalDate> oldestEntry = null;
        for (Map.Entry<AID, LocalDate> entry : aidDates.entrySet()) {
            if (oldestEntry == null || entry.getValue().isBefore(oldestEntry.getValue())) {
                oldestEntry = entry;
            }
        }

        return oldestEntry.getKey();
    }

    private void acceptAppointment(AID repairCoffee) throws IOException {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(repairCoffee);
        message.setConversationId("rdv accepte");
        message.setContentObject(productToRepair);
        send(message);
    }

    public PartWithStore getStoreWithBestPriceForPart(List<PartWithStore> partsWithStore) {
        if (partsWithStore == null || partsWithStore.isEmpty()) {
            return null; // Retourne null si la liste est vide ou non initialisée
        }

        PartWithStore bestPricePartWithStore = partsWithStore.get(0); // Initialiser avec le premier élément

        for (PartWithStore partWithStore : partsWithStore) {
            if (partWithStore.getPartToSell().getStandardPrice() < bestPricePartWithStore.getPartToSell().getStandardPrice()) {
                bestPricePartWithStore = partWithStore; // Mettre à jour si un prix inférieur est trouvé
            }
        }

        println("le meilleur store est : " + bestPricePartWithStore.getPartStore().getLocalName() + " et propose la pièce suivante : " + bestPricePartWithStore.getPartToSell().toString());

        return bestPricePartWithStore;
    }

    private void buyPart(PartWithStore partWithStore){
        ACLMessage buyRequest = new ACLMessage(ACLMessage.REQUEST);
        buyRequest.setContent(productToRepair.getDefault().toString());
        buyRequest.setConversationId("j'achete la piece");
        buyRequest.addReceiver(partWithStore.getPartStore());
        products.remove(productToRepair);
        wallet -= partWithStore.getPartToSell().getStandardPrice();
        println("j'achète la pièce, il me reste tant : " + wallet);
        send(buyRequest);
    }

    // Méthode pour retirer de l'argent du portefeuille de l'utilisateur
    public void withdrawMoney(double amount) {
        if (amount > 0 && this.wallet >= amount) {
            this.wallet -= amount;
            println("Montant de " + amount + "€ retiré du portefeuille. Nouveau solde : " + this.wallet + "€");
        } else {
            println("Opération de retrait échouée. Vérifiez que le montant est positif et que le solde est suffisant.");
        }
    }

    // Méthode pour retirer un produit de la liste des produits de l'utilisateur
    public void removeProduct(Product productToRemove) {
        if (products.contains(productToRemove)) {
            products.remove(productToRemove);
            println("Produit retiré : " + productToRemove.getName());
        } else {
            println("Produit non trouvé dans la liste et ne peut être retiré.");
        }
    }

    public void givePart(ACLMessage message){
        ACLMessage repairRequest = new ACLMessage(ACLMessage.REQUEST);
        println("je veux bien que voue le réparez");
        repairRequest.setConversationId("reparez");
        repairRequest.addReceiver(message.getSender());
        send(repairRequest);
    }

    private void noMoreChoice(){
        if(wallet >= productToRepair.getPrice()) {
            wallet -= productToRepair.getPrice();
            println("Dommage, J'achèterai un nouveau produit auprès des distributeurs pour la modique somme de  " + productToRepair.getPrice() + "€");
        }
        else println("Dommage, je suis pauvre, je ne peux pas acheter de nouveau produit ... ;( ");
        terminateRepair();
    }



}
