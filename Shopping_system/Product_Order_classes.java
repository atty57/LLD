// Product Catalog - Singleton pattern for centralized product management
public class ProductCatalog {
    private static ProductCatalog instance;
    private String catalogId;
    private String name;
    private List<Product> products;
    private Map<String, List<Product>> categoryProductMap;
    private Date lastUpdated;
    
    private ProductCatalog() {
        this.catalogId = generateCatalogId();
        this.name = "Main Product Catalog";
        this.products = new ArrayList<>();
        this.categoryProductMap = new HashMap<>();
        this.lastUpdated = new Date();
    }
    
    public static ProductCatalog getInstance() {
        if (instance == null) {
            synchronized (ProductCatalog.class) {
                if (instance == null) {
                    instance = new ProductCatalog();
                }
            }
        }
        return instance;
    }
    
    public void addProduct(Product product) {
        products.add(product);
        // Add to category map
        String categoryName = product.getCategory().getName();
        categoryProductMap.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(product);
        lastUpdated = new Date();
    }
    
    public void removeProduct(String productId) {
        products.removeIf(p -> p.getProductId().equals(productId));
        // Also remove from category map
        for (List<Product> categoryProducts : categoryProductMap.values()) {
            categoryProducts.removeIf(p -> p.getProductId().equals(productId));
        }
        lastUpdated = new Date();
    }
    
    public List<Product> searchProducts(String query) {
        // Simple search implementation - in real world, this would use advanced search algorithms
        return products.stream()
            .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()) ||
                        p.getDescription().toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    public List<Product> getProductsByCategory(String categoryName) {
        return categoryProductMap.getOrDefault(categoryName, new ArrayList<>());
    }
}

// Product class representing items for sale
public class Product {
    private String productId;
    private String name;
    private String description;
    private double price;
    private List<String> availableSizes;
    private List<String> availableColors;
    private ProductCategory category;
    private int stockQuantity;
    private String sellerId;
    private List<String> images;
    private double rating;
    private int reviewCount;
    private ProductStatus status;  // AVAILABLE, OUT_OF_STOCK, DISCONTINUED
    
    public Product(String name, double price, ProductCategory category, String sellerId) {
        this.productId = generateProductId();
        this.name = name;
        this.price = price;
        this.category = category;
        this.sellerId = sellerId;
        this.availableSizes = new ArrayList<>();
        this.availableColors = new ArrayList<>();
        this.images = new ArrayList<>();
        this.rating = 0.0;
        this.reviewCount = 0;
        this.status = ProductStatus.AVAILABLE;
    }
    
    public boolean isAvailable() {
        return status == ProductStatus.AVAILABLE && stockQuantity > 0;
    }
    
    public void updateStock(int quantity) {
        this.stockQuantity = quantity;
        if (stockQuantity <= 0) {
            status = ProductStatus.OUT_OF_STOCK;
        } else {
            status = ProductStatus.AVAILABLE;
        }
    }
    
    public void addReview(double rating) {
        // Update average rating
        this.rating = ((this.rating * reviewCount) + rating) / (reviewCount + 1);
        this.reviewCount++;
    }
}

// Product Category class
public class ProductCategory {
    private String categoryId;
    private String name;
    private String description;
    private ProductCategory parentCategory;  // For hierarchical categories
    private List<ProductCategory> subCategories;
    
    public ProductCategory(String name, String description) {
        this.categoryId = generateCategoryId();
        this.name = name;
        this.description = description;
        this.subCategories = new ArrayList<>();
    }
    
    public void addSubCategory(ProductCategory subCategory) {
        subCategories.add(subCategory);
        subCategory.setParentCategory(this);
    }
    
    public String getFullCategoryPath() {
        if (parentCategory == null) {
            return name;
        }
        return parentCategory.getFullCategoryPath() + " > " + name;
    }
}

// Shopping Cart class
public class ShoppingCart {
    private String cartId;
    private User owner;  // Cart cannot exist without a user
    private List<Item> items;
    private double totalAmount;
    private Date lastModified;
    
    public ShoppingCart(User owner) {
        this.cartId = generateCartId();
        this.owner = owner;
        this.items = new ArrayList<>();
        this.totalAmount = 0.0;
        this.lastModified = new Date();
    }
    
    public void addItem(Product product, int quantity, String size, String color) {
        // Check if item already exists
        Item existingItem = findItem(product.getProductId(), size, color);
        
        if (existingItem != null) {
            existingItem.updateQuantity(existingItem.getQuantity() + quantity);
        } else {
            Item newItem = new Item(product, quantity, size, color);
            items.add(newItem);
        }
        
        recalculateTotal();
        lastModified = new Date();
    }
    
    public void removeItem(String itemId) {
        items.removeIf(item -> item.getItemId().equals(itemId));
        recalculateTotal();
        lastModified = new Date();
    }
    
    public void updateItemQuantity(String itemId, int newQuantity) {
        Item item = items.stream()
            .filter(i -> i.getItemId().equals(itemId))
            .findFirst()
            .orElse(null);
            
        if (item != null) {
            if (newQuantity <= 0) {
                removeItem(itemId);
            } else {
                item.updateQuantity(newQuantity);
                recalculateTotal();
                lastModified = new Date();
            }
        }
    }
    
    public void clear() {
        items.clear();
        totalAmount = 0.0;
        lastModified = new Date();
    }
    
    private void recalculateTotal() {
        totalAmount = items.stream()
            .mapToDouble(Item::getSubtotal)
            .sum();
    }
    
    public List<Item> getItems() {
        return new ArrayList<>(items);  // Return a copy to prevent external modification
    }
}

// Item class representing products in cart or order
public class Item {
    private String itemId;
    private Product product;
    private int quantity;
    private String selectedSize;
    private String selectedColor;
    private double priceAtTimeOfAddition;  // Price might change, so we store it
    
    public Item(Product product, int quantity, String size, String color) {
        this.itemId = generateItemId();
        this.product = product;
        this.quantity = quantity;
        this.selectedSize = size;
        this.selectedColor = color;
        this.priceAtTimeOfAddition = product.getPrice();
    }
    
    public double getSubtotal() {
        return priceAtTimeOfAddition * quantity;
    }
    
    public void updateQuantity(int newQuantity) {
        if (newQuantity > 0) {
            this.quantity = newQuantity;
        }
    }
}

// Order class
public class Order {
    private String orderId;
    private User user;  // Order cannot exist without a user
    private List<Item> items;
    private Address deliveryAddress;
    private OrderStatus status;
    private Date orderDate;
    private double totalAmount;
    private Payment payment;
    private Invoice invoice;
    private Status shipmentStatus;
    
    public Order(User user, List<Item> items) {
        this.orderId = generateOrderId();
        this.user = user;
        this.items = new ArrayList<>(items);  // Create a copy
        this.orderDate = new Date();
        this.status = OrderStatus.PENDING_PAYMENT;
        calculateTotalAmount();
    }
    
    private void calculateTotalAmount() {
        totalAmount = items.stream()
            .mapToDouble(Item::getSubtotal)
            .sum();
    }
    
    public void setDeliveryAddress(Address address) {
        this.deliveryAddress = address;
    }
    
    public void processPayment(Payment payment) {
        if (payment.processPayment(totalAmount)) {
            this.payment = payment;
            this.status = OrderStatus.PAID;
            generateInvoice();
            createShipmentStatus();
            sendOrderConfirmationNotification();
        } else {
            this.status = OrderStatus.PAYMENT_FAILED;
        }
    }
    
    private void generateInvoice() {
        this.invoice = new Invoice(this);
    }
    
    private void createShipmentStatus() {
        this.shipmentStatus = new Status(this);
    }
    
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING_PAYMENT || 
               status == OrderStatus.PAID ||
               status == OrderStatus.PROCESSING;
    }
    
    public void cancel() {
        if (canBeCancelled()) {
            status = OrderStatus.CANCELLED;
            // Process refund if payment was made
            if (payment != null) {
                payment.refund();
            }
        }
    }
}

// Invoice class
public class Invoice {
    private String invoiceId;
    private Order order;  // Invoice cannot exist without an order
    private Date invoiceDate;
    private double amount;
    private double tax;
    private double shippingCharges;
    private double discount;
    private double finalAmount;
    private String invoiceUrl;  // PDF URL
    
    public Invoice(Order order) {
        this.invoiceId = generateInvoiceId();
        this.order = order;
        this.invoiceDate = new Date();
        this.amount = order.getTotalAmount();
        calculateCharges();
        generateInvoicePDF();
    }
    
    private void calculateCharges() {
        // Calculate tax, shipping, discounts
        this.tax = amount * 0.18;  // 18% GST
        this.shippingCharges = amount > 500 ? 0 : 40;  // Free shipping above 500
        this.discount = calculateDiscount();
        this.finalAmount = amount + tax + shippingCharges - discount;
    }
    
    private double calculateDiscount() {
        // Apply any applicable discounts
        return 0.0;  // Simplified for now
    }
    
    private void generateInvoicePDF() {
        // Generate PDF and store URL
        this.invoiceUrl = "https://invoices.amazon.com/" + invoiceId + ".pdf";
    }
}