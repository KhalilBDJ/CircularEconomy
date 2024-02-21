package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.data.Part;
import handsOn.circularEconomy.data.Product;
import handsOn.circularEconomy.data.ProductType;
import jade.core.AID;
import jade.core.AgentServicesTools;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepairCoffeeAgent extends AgentWindowed {
    List<ProductType> specialities;
    private AID currentClient;
    private Map<AID, Part> partWithStore;
    private double bestPrice;
    private int partStoreNumber;
    private int check;
    private int noStoreCount;

    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(),this);
        currentClient = new AID();
        partWithStore = new HashMap<>();
        bestPrice = 1000000000;
        partStoreNumber = 4;
        check = 0;
        noStoreCount = 0;
        this.window.setBackgroundTextColor(Color.orange);
        println("hello, do you want coffee ?");
        var hasard = new Random();
        specialities = new ArrayList<>();
        for(ProductType type : ProductType.values())
            if(hasard.nextBoolean()) specialities.add(type);
        //we need at least one speciality
        if(specialities.isEmpty()) specialities.add(ProductType.values()[hasard.nextInt(ProductType.values().length)]);
        println("I have these specialities : ");
        specialities.forEach(p->println("\t"+p));
        //registration to the yellow pages (Directory Facilitator Agent)
        AgentServicesTools.register(this, "repair", "coffee");
        println("I'm just registered as a repair-coffee");

        addBehaviour(new CyclicBehaviour(this){
            public void action(){
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (message != null){
                    switch (message.getConversationId()) {
                        case "puis-je avoir de l'aide ?":
                            try {
                                canRepair(message);
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case "rdv accepte":
                            println("je vérifie ça...");
                            try {
                                checkIfRepairable(message);
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                    }
                }
                else block();
            }
        });

        addBehaviour(new CyclicBehaviour(this){
            public void action(){
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (message != null){
                    switch (message.getConversationId()){
                        case "j'ai la piece":
                            Part part = fromString(message.getContent());
                            check += 1;
                            partWithStore.put(message.getSender(), part);
                            if (check == partStoreNumber){
                                getStoreWithBestPriceForPart(partWithStore);
                                partWithStore = new HashMap<>();
                                check = 0;
                            }
                            break;
                        case "je n'ai pas la piece":
                            check += 1;
                            noStoreCount += 1;
                            if (check == partStoreNumber){
                                getStoreWithBestPriceForPart(partWithStore);
                                partWithStore = new HashMap<>();
                                check =0;
                            }
                            if (noStoreCount == partStoreNumber){
                                println("aucun magasin n'a la pièce");
                                noStoreCount = 0;
                                check = 0;
                            }
                            break;
                    }

                }
            }
        });
    }

    private void canRepair(ACLMessage message) throws UnreadableException, IOException {
        Product currentProduct = (Product) message.getContentObject();
        for(var speciality: specialities){
            if (currentProduct.getType().equals(speciality)){
                println("oui je peux aider");
                ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                response.addReceiver(message.getSender());
                response.setContent(this.getLocalName());
                response.setConversationId("je peux aider");
                send(response);

                ACLMessage proposition = new ACLMessage(ACLMessage.INFORM);

                proposition.addReceiver(message.getSender());
                Random random = new Random();
                LocalDate date = LocalDate.now().plusDays(random.nextInt(3) + 1);
                println("je vous propose cette date pour vérifier le produit : " + date.toString());
                proposition.setContent(date.toString());
                proposition.setConversationId("proposition de date");
                send(proposition);

                return;
            }
        }
        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
        response.addReceiver(message.getSender());
        response.setContent(this.getLocalName());
        response.setConversationId("je ne peux pas aider");
        send(response);


        println("je n'ai pas la specialité");

    }

    private void checkIfRepairable(ACLMessage message) throws UnreadableException {
        currentClient = message.getSender();
        Product productToRepair = (Product) message.getContentObject();
        if (productToRepair.getBreakdownLevel() == 4){
            println("Monsieur je suis sincérement navré, mais votre objet est foutu...");
        }
        else {
            println("Bonne nouvelle pour vous monsieur, vous avez la possibilité de le réparer, CEPENDANT, j'espère que vous avec le portfeuille bien fourni");
            var partsStores = AgentServicesTools.searchAgents(this, "repair", "partstore");
            for (var partStore : partsStores){
                ACLMessage priceForPart = new ACLMessage(ACLMessage.REQUEST);
                priceForPart.addReceiver(partStore);
                priceForPart.setConversationId("est-ce que y'a des pieces pour cet objet ?");
                priceForPart.setContent(productToRepair.getDefault().getName());
                send(priceForPart);
            }

        }
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


    private Map<AID, Part> getStoreWithBestPriceForPart(Map<AID, Part> stores) {
        // Vérifier si la map est vide
        if (stores == null || stores.isEmpty()) {
            return new HashMap<>(); // ou retourner null selon le besoin de l'application
        }

        // Initialiser les variables pour trouver le meilleur prix
        AID bestStoreAID = null;
        Part bestPricePart = null;
        double bestPrice = Double.MAX_VALUE;

        // Parcourir la map pour trouver le meilleur prix
        for (Map.Entry<AID, Part> entry : stores.entrySet()) {
            Part part = entry.getValue();
            if (part.getStandardPrice() < bestPrice) {
                bestPrice = part.getStandardPrice();
                bestStoreAID = entry.getKey();
                bestPricePart = part;
            }
        }

        // Créer et retourner le résultat
        Map<AID, Part> result = new HashMap<>();
        if (bestStoreAID != null) { // S'assurer qu'une Part a été trouvée
            result.put(bestStoreAID, bestPricePart);
        }

        println("le meilleur magasin est : " + bestStoreAID.getLocalName() + " et propose cette pièce : " + bestPricePart.toString());
        return result;
    }


}
