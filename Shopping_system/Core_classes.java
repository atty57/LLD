// Base Account class that represents any user in the system
public abstract class Account {
    private String accountId;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private Date dateOfBirth;
    private List<Address> addresses;  // Composition - addresses cannot exist without account
    private List<Card> paymentCards;  // Composition - cards cannot exist without account
    private List<Banking> bankAccounts;
    private Date createdAt;
    private AccountStatus status;  // ACTIVE, BLOCKED, SUSPENDED
    
    // Constructor and methods
    public Account(String name, String email, String phoneNumber) {
        this.accountId = generateAccountId();
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.addresses = new ArrayList<>();
        this.paymentCards = new ArrayList<>();
        this.bankAccounts = new ArrayList<>();
        this.createdAt = new Date();
        this.status = AccountStatus.ACTIVE;
    }
    
    public void addAddress(Address address) {
        addresses.add(address);
    }
    
    public void addPaymentCard(Card card) {
        paymentCards.add(card);
    }
    
    public void addBankAccount(Banking bankAccount) {
        bankAccounts.add(bankAccount);
    }
    
    // Abstract methods to be implemented by subclasses
    public abstract boolean canModifyProduct();
    public abstract boolean canBlockUser();
}

// User class - represents regular buyers
public class User extends Account {
    private ShoppingCart cart;
    private List<Order> orders;
    private List<Product> wishlist;
    private List<Product> recentlyViewed;
    
    public User(String name, String email, String phoneNumber) {
        super(name, email, phoneNumber);
        this.cart = new ShoppingCart(this);
        this.orders = new ArrayList<>();
        this.wishlist = new ArrayList<>();
        this.recentlyViewed = new ArrayList<>();
    }
    
    public Order placeOrder() {
        // Create order from current cart items
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cannot place order with empty cart");
        }
        Order order = new Order(this, cart.getItems());
        orders.add(order);
        cart.clear();
        return order;
    }
    
    public void cancelOrder(String orderId) {
        Order order = findOrderById(orderId);
        if (order != null && order.canBeCancelled()) {
            order.cancel();
        }
    }
    
    public OrderStatus checkOrderStatus(String orderId) {
        Order order = findOrderById(orderId);
        return order != null ? order.getStatus() : null;
    }
    
    @Override
    public boolean canModifyProduct() {
        return false;  // Regular users cannot modify products
    }
    
    @Override
    public boolean canBlockUser() {
        return false;  // Regular users cannot block other users
    }
}

// Admin class - represents platform administrators
public class Admin extends Account {
    private List<String> permissions;
    private String adminLevel;  // SUPER_ADMIN, MODERATOR, etc.
    
    public Admin(String name, String email, String phoneNumber, String adminLevel) {
        super(name, email, phoneNumber);
        this.adminLevel = adminLevel;
        this.permissions = new ArrayList<>();
        initializePermissions();
    }
    
    public void addProduct(Product product) {
        // Logic to add product to catalog
        ProductCatalog.getInstance().addProduct(product);
    }
    
    public void removeProduct(String productId) {
        // Logic to remove product from catalog
        ProductCatalog.getInstance().removeProduct(productId);
    }
    
    public void blockUser(String userId) {
        // Logic to block a user account
        // This would typically interact with a user management service
    }
    
    @Override
    public boolean canModifyProduct() {
        return permissions.contains("MODIFY_PRODUCT");
    }
    
    @Override
    public boolean canBlockUser() {
        return permissions.contains("BLOCK_USER");
    }
}

// Address class - represents delivery addresses
public class Address {
    private String addressId;
    private String houseNumber;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private AddressType type;  // HOME, OFFICE, OTHER
    private boolean isDefault;
    
    public Address(String houseNumber, String street, String city, 
                   String state, String zipCode, String country) {
        this.addressId = generateAddressId();
        this.houseNumber = houseNumber;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.isDefault = false;
    }
    
    public String getFullAddress() {
        return String.format("%s, %s, %s, %s - %s, %s", 
            houseNumber, street, city, state, zipCode, country);
    }
}

// Base Card class for payment cards
public abstract class Card {
    protected String cardId;
    protected String cardholderName;
    protected String cardNumber;
    protected String cvv;
    protected Date expiryDate;
    protected CardStatus status;  // ACTIVE, EXPIRED, BLOCKED
    
    public Card(String cardholderName, String cardNumber, String cvv, Date expiryDate) {
        this.cardId = generateCardId();
        this.cardholderName = cardholderName;
        this.cardNumber = maskCardNumber(cardNumber);
        this.cvv = cvv;  // Should be encrypted in real implementation
        this.expiryDate = expiryDate;
        this.status = CardStatus.ACTIVE;
    }
    
    public abstract boolean processPayment(double amount);
    public abstract double getTransactionLimit();
    
    protected String maskCardNumber(String cardNumber) {
        // Show only last 4 digits
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

// Debit Card implementation
public class DebitCard extends Card {
    private double dailyLimit;
    private String linkedAccountNumber;
    
    public DebitCard(String cardholderName, String cardNumber, 
                     String cvv, Date expiryDate, String linkedAccountNumber) {
        super(cardholderName, cardNumber, cvv, expiryDate);
        this.linkedAccountNumber = linkedAccountNumber;
        this.dailyLimit = 50000.0;  // Default daily limit
    }
    
    @Override
    public boolean processPayment(double amount) {
        // Check if payment can be processed
        // In real implementation, this would connect to payment gateway
        return amount <= dailyLimit && status == CardStatus.ACTIVE;
    }
    
    @Override
    public double getTransactionLimit() {
        return dailyLimit;
    }
}

// Credit Card implementation
public class CreditCard extends Card {
    private double creditLimit;
    private double availableCredit;
    private double minimumPayment;
    
    public CreditCard(String cardholderName, String cardNumber, 
                      String cvv, Date expiryDate, double creditLimit) {
        super(cardholderName, cardNumber, cvv, expiryDate);
        this.creditLimit = creditLimit;
        this.availableCredit = creditLimit;
        this.minimumPayment = 0.0;
    }
    
    @Override
    public boolean processPayment(double amount) {
        if (amount <= availableCredit && status == CardStatus.ACTIVE) {
            availableCredit -= amount;
            minimumPayment = calculateMinimumPayment();
            return true;
        }
        return false;
    }
    
    @Override
    public double getTransactionLimit() {
        return availableCredit;
    }
    
    private double calculateMinimumPayment() {
        // Simplified calculation
        return (creditLimit - availableCredit) * 0.05;  // 5% of used credit
    }
}

// Banking class for net banking payments
public class Banking {
    private String bankingId;
    private String accountNumber;
    private String bankName;
    private String ifscCode;
    private BankingStatus status;
    
    public Banking(String accountNumber, String bankName, String ifscCode) {
        this.bankingId = generateBankingId();
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.ifscCode = ifscCode;
        this.status = BankingStatus.ACTIVE;
    }
    
    public boolean initiatePayment(double amount) {
        // Logic to initiate net banking payment
        return status == BankingStatus.ACTIVE;
    }
}