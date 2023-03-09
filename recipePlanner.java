
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyManagementException;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.ToDoubleBiFunction;

public class recipePlanner {

    Scanner in;

    recipePlanner(String name, Scanner in, int request) {
        this.in = in;
        this.maxDepth = 0;
        knownRecipes = new TreeMap<String, recipe>();
        totals = new TreeMap<String, Integer>();
        excess = new TreeMap<String, Integer>();
        this.initialRequest = request;
        this.itemRecipe = new recipe(name, null);
        readRecipesFromFile();
        if(knownRecipes.containsKey(name)){
            this.itemRecipe = knownRecipes.get(name);
        }
        else{
            System.out.println(name+" has no recipe");
        }

    }

    TreeMap<String, recipe> knownRecipes;
    TreeMap<String, Integer> totals;
    TreeMap<String, Integer> excess;
    recipe itemRecipe;
    int initialRequest;
    int maxDepth = 0;

    void readRecipesFromFile() {
        File file = new File("recipeLibrary.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String Comment = null;

            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                if (line.charAt(0) == '#') {
                    // This is a new recipe

                    String[] terms = line.split(",");
                    String itemName = terms[0].substring(2);
                    int yield = Integer.parseInt(terms[1].trim());
                    //System.out.println("Learning Recipe: "+yield+"x "+itemName);
                    TreeMap<String, Integer> subRecipes = new TreeMap<>();
                    TreeMap<String, Integer> biProduct = new TreeMap<>();
                    while ((line = br.readLine()) != null && line.length()>0) {
                        if (line.charAt(0) == '|') {
                            terms = line.split(",");
                            String subitemName = terms[0].substring(2);
                            int subCount = Integer.parseInt(terms[1].trim());
                            subRecipes.put(subitemName, subCount);
                           //System.out.println("|--"+subCount+"x"+subitemName);
                        }
                        if(line.charAt(0) == '+'){
                            Comment = line.substring(2);
                        }
                        if (line.charAt(0)== '>'){
                            terms = line.split(",");
                            String biProductName = terms[0].substring(2);
                            int biProductYield = Integer.parseInt(terms[1].trim());
                            biProduct.put(biProductName, biProductYield);
                        }
                        if (line.charAt(0) == '-') {
                            break;
                        }
                    }
                    recipe temp = new recipe(itemName, null, yield,subRecipes,biProduct);
                    if(Comment != null){
                        temp.Comment = Comment;
                    }

                    knownRecipes.put(itemName, temp);
                    

                }

            }

        } catch (Exception e) {
            System.out.println("There was a problem reading the file");
            e.printStackTrace();
        }

    }

    class recipe implements Comparable<recipe> {

        String name;
        recipe parent;
        // int count;
        int yield;
        int depth;
        boolean raw = false;
        TreeMap<String, Integer> subRecipes = new TreeMap<String, Integer>();
        TreeMap<String, Integer> biProducts = new TreeMap<String, Integer>();
        TreeMap<String, Double> subtotal = new TreeMap<String, Double>();
        String Comment;

        recipe(String name, recipe parent) {
            this.name = name;
            this.parent = parent;
            this.yield = 1;

        }

        recipe(String name, recipe parent, int yield) {
            this.name = name;
            this.parent = parent;
            this.yield = yield;

        }

        recipe(String name, recipe parent, int yield, TreeMap<String, Integer> subRec, TreeMap<String, Integer> biProduct) {
            this.name = name;
            this.parent = parent;
            this.yield = yield;
            this.subRecipes = subRec;
            this.biProducts = biProduct;
        }

        boolean isDefined() {
            return (knownRecipes.get(this.name) != null);
        }

        @Override
        public boolean equals(Object o) {

            if (o == this) {
                return true;
            }
            if (o instanceof recipe) {
                recipe otherRec = (recipe) o;
                if (this.name.equalsIgnoreCase(otherRec.name)) {
                    return true;
                }
            }

            return false;
        }

        void accumulateTraverse(int multiplier) {

            if (subRecipes.isEmpty()) {
                //System.out.println(this.name+ " has no subrecipes");
                return;
            }
            for (String subrecipe : subRecipes.keySet()) {
                recipe sub = knownRecipes.get(subrecipe);
                if(sub == null){
                    //System.out.println(subrecipe+ " has no recipe");
                    sub = new recipe(subrecipe,this, 1 );
                    sub.Comment = "Raw Material";
                    sub.raw = true;
                    knownRecipes.put(subrecipe, sub);

                }
                //System.out.println("Sub is "+sub.name);
                if (this.depth + 1 > sub.depth) {
                    sub.depth = this.depth + 1;
                    if (sub.depth > maxDepth) {
                        maxDepth = sub.depth;
                    }
                }
                sub.addToResourceList(multiplier, subRecipes.get(subrecipe));
                
            }
        }

        void displayExcess() {
            System.out.println("----------Excess Materials-----------------");
            System.out.println();
            for (String component : excess.keySet()) {
                System.out.println(component + " " + excess.get(component));
            }
            System.out.println();
        }

        void addToResourceList(int multiplier, int subCount) {
            //System.out.println("Adding Resources for: "+name);
            String component = name;
            // System.out.println("Adding "+name+ " to resource list " + multiplier + "
            // times");
            int available = 0;
            if (excess.keySet().contains(component)) {
                available = excess.get(component);
            }
            int request = subCount * multiplier;
            int need = request - available;

            if (need <= 0) {
                
                if(need != 0){
                    excess.put(component, need * -1);
                }
               
                
                if(totals.containsKey(component)){
                    totals.put(component,totals.get(component)+ request);
                }
                else{
                    totals.put(component, request);
                }
                
                
                return;
            }

            int crafts = (int) Math.ceil((1.0 * need) / (1.0 * yield));
            // System.out.println("Yield:"+yield);
            // System.out.println("Crafting " +name+" "+crafts+" times");
            int made = crafts * yield;
            int newExcess = made - need;
            
            for (String biProduct : biProducts.keySet()) {
                recipe temp = knownRecipes.get(biProduct);

                int producedBiProduct = crafts * temp.yield;

                if(excess.containsKey(biProduct)){
                    excess.put(biProduct,excess.get(biProduct)+ producedBiProduct);
                }
                else{
                    excess.put(biProduct, producedBiProduct);
                }
                
            }

            if (newExcess == 0) {
                excess.remove(component);
            } else {
                excess.put(component, newExcess);
            }

            if (totals.keySet().contains(component)) {
                totals.put(component, totals.get(component) + request);

            } else {
                totals.put(component, request);
            }
            
            this.accumulateTraverse(crafts); // *multiplier /

        }

        void printTotals() {

            System.out.println("\nCrafting " + initialRequest + " " + itemRecipe.name + " requires:");
            System.out.println("---------------Recipe Breakdown------------");
            System.out.println();
            for (int i = maxDepth; i >= 0; i--) {
                System.out.println("Depth: "+i);
                System.out.println();
                for (String current : totals.keySet()) {

                    recipe item = knownRecipes.get(current);
                    
                    if(item == null){
                        System.out.println(itemRecipe.name+" is an unknown recipe");
                        return;
                    }
                    if (item.depth == i) {
                        System.out.print(item.name + ": " + totals.get(item.name));
                        if(item.Comment != null){
                            System.out.print(" #"+item.Comment);
                        }
                        System.out.println();
                    }

                }
                System.out.println();
            }
            System.out.println("-------RAW MATERIAL LIST-----------");
            for (String current : totals.keySet()) {
                recipe item = knownRecipes.get(current);
                if(item.raw){
                    System.out.println(item.name+": "+ totals.get(item.name));
                }
            }
            System.out.println();
            

        }

        @Override
        public int compareTo(recipe o) {
            return this.name.compareTo(o.name);
        }

    }

    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);

        System.out.println("What item would you like to craft? Please type item name and press Enter");
        String item = in.nextLine();
        System.out.println("How many " + item + " would you like to craft");
        int requested = Integer.parseInt(in.nextLine());
        recipePlanner myRec = new recipePlanner(item, in, requested);

        myRec.itemRecipe.addToResourceList(1, requested); // accumulate
        myRec.itemRecipe.printTotals();
        myRec.itemRecipe.displayExcess();

        // 

        in.close();
    }
}
