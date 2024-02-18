package handsOn.circularEconomy.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** a reparable product is identified by a name and an id.
 * it owns a type and a price
 * This class creates list of products that can be used in the project
 * @author emmanueladam
 * */
public class Product implements Serializable {

    static int nbProducts = 0;
    long id;
    int breakdownLevel;
    String name;
    ProductType type;
    double price;
    public static final int NB_PRODS = 100;
    public static List<Product> listProducts;
    Part faultyPart;


    Product(String name, ProductType type){
        this.name = name;
        this.type = type;
        price = type.getStandardPrice()*(1.+Math.random()*.2);
        id = ++nbProducts;
        Random random = new Random();
        this.breakdownLevel = random.nextInt(5);
    }

    @Override
    public String toString() {
        return String.format("Product{ %d : %s - %s - %.2f€}", id, name, type, price);
    }



    static public List<Product> getListProducts() {
        if (listProducts == null) {
            listProducts = new ArrayList<>(NB_PRODS);
            int nbSpec = ProductType.values().length;
            int nbBySpec = NB_PRODS /nbSpec;
            var listeType = ProductType.values();
            for(var type:listeType){
                for(int i=0; i<nbBySpec; i++) {
                    listProducts.add(new Product(type+"-"+ i, type));
                }
            }
        }
        return listProducts;
    }


    /**function that select which part to repair
     * @return part selected randomly from the existing list of parts*/
    public Part getDefault() {
        if(faultyPart==null) {
            //choose a part
            var flux = Part.getListParts().stream().filter(p -> p.getType() == type);
            var l = flux.toList();
            faultyPart = l.get(new Random().nextInt(l.size()));
        }
        return faultyPart;
    }

    public String getName() {
        return name;
    }

    public ProductType getType() {
        return type;
    }
    public double getPrice() {
        return price;
    }

    /**check the creation of the list of products.
     * (you can store the creation in a file and reload it later)*/

    public static void main(String[] args)
    {
        var tab = Product.getListProducts();
        for(var p:tab)  System.out.println(p);
        System.out.println("-".repeat(20));
    }

    public int getBreakdownLevel() {
        return breakdownLevel;
    }

    public void setBreakdownLevel(int breakdownLevel) {
        this.breakdownLevel = breakdownLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(name, product.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
