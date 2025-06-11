// Base Payment class
public abstract class Payment {
    protected String paymentId;
    protected double amount;
    protected PaymentStatus status;  // PENDING, SUCCESS, FAILED, REFUNDED
    protected Date paymentDate;
    protected String transactionId;
    
    public Payment(double amount) {
        this.paymentId = generatePaymentId();
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.paymentDate = new Date();
    }
    
    // Template method pattern - common flow with specific implementation
    public boolean processPayment(double orderAmount) {
        if (orderAmount != amount) {
            return false;  // Amount mismatch
        }
        
        boolean result = executePayment();
        
        if (result) {
            status = PaymentStatus.SUCCESS;
            transactionId = generateTransactionId();
        } else {
            status = PaymentStatus.FAILED;
        }
        
        return result;
    }
    
    // Abstract method to be implemented by subclasses
    protected abstract boolean executePayment();
    
    public boolean refund() {
        if (status == PaymentStatus.SUCCESS) {
            boolean refundResult = executeRefund();
            if (refundResult) {
                status = PaymentStatus.REFUNDED;
            }
            return refundResult;
        }
        return false;
    }
    
    protected abstract boolean executeRefund();
}

// Cash Payment implementation
public class CashPayment extends Payment {
    private boolean cashCollected;
    private String collectionAgentId;
    
    public CashPayment(double amount) {
        super(amount);
        this.cashCollected = false;
    }
    
    @Override
    protected boolean executePayment() {
        // For cash on delivery, payment is marked as pending until delivery
        // This just validates that COD is available for the order
        return true;  // COD is always "successful" at order time
    }
    
    @Override
    protected boolean executeRefund() {
        // Cash refunds need special handling
        return false;  // Cannot refund cash payments automatically
    }
    
    public void markCashCollected(String agentId) {
        this.cashCollected = true;
        this.collectionAgentId = agentId;
        this.status = PaymentStatus.SUCCESS;
    }
}

// Debit Card Payment implementation
public class DebitCardPayment extends Payment {
    private DebitCard debitCard;
    private String authorizationCode;
    
    public DebitCardPayment(double amount, DebitCard debitCard) {
        super(amount);
        this.debitCard = debitCard;
    }
    
    @Override
    protected boolean executePayment() {
        // Use the debit card to process payment
        boolean result = debitCard.processPayment(amount);
        if (result) {
            authorizationCode = generateAuthCode();
        }
        return result;
    }
    
    @Override
    protected boolean executeRefund() {
        // Process refund to debit card
        // In real implementation, this would connect to payment gateway
        return true;
    }
}

// Credit Card Payment implementation
public class CreditCardPayment extends Payment {
    private CreditCard creditCard;
    private String authorizationCode;
    private int emiMonths;  // For EMI payments
    
    public CreditCardPayment(double amount, CreditCard creditCard) {
        super(amount);
        this.creditCard = creditCard;
        this.emiMonths = 0;  // No EMI by default
    }
    
    public void setEmiMonths(int months) {
        this.emiMonths = months;
    }
    
    @Override
    protected boolean executePayment() {
        double paymentAmount = emiMonths > 0 ? calculateEmiAmount() : amount;
        boolean result = creditCard.processPayment(paymentAmount);
        if (result) {
            authorizationCode = generateAuthCode();
        }
        return result;
    }
    
    @Override
    protected boolean executeRefund() {
        // Process refund to credit card
        return true;
    }
    
    private double calculateEmiAmount() {
        // Simplified EMI calculation
        double interestRate = 0.12 / 12;  // 12% annual rate
        return (amount * interestRate * Math.pow(1 + interestRate, emiMonths)) / 
               (Math.pow(1 + interestRate, emiMonths) - 1);
    }
}

// Base Notification class
public abstract class Notification {
    protected String notificationId;
    protected String description;
    protected Date timestamp;
    protected NotificationType type;  // ORDER_PLACED, SHIPMENT_UPDATE, DELIVERY, etc.
    protected NotificationStatus status;  // PENDING, SENT, FAILED
    
    public Notification(String description, NotificationType type) {
        this.notificationId = generateNotificationId();
        this.description = description;
        this.type = type;
        this.timestamp = new Date();
        this.status = NotificationStatus.PENDING;
    }
    
    // Template method for sending notifications
    public boolean send() {
        if (validate()) {
            boolean result = sendNotification();
            status = result ? NotificationStatus.SENT : NotificationStatus.FAILED;
            return result;
        }
        return false;
    }
    
    protected abstract boolean validate();
    protected abstract boolean sendNotification();
}

// Email Notification implementation
public class EmailNotification extends Notification {
    private String emailId;
    private String subject;
    private String htmlBody;
    private List<String> attachments;
    
    public EmailNotification(String emailId, String subject, 
                           String description, NotificationType type) {
        super(description, type);
        this.emailId = emailId;
        this.subject = subject;
        this.attachments = new ArrayList<>();
    }
    
    @Override
    protected boolean validate() {
        // Validate email format
        return emailId != null && emailId.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    @Override
    protected boolean sendNotification() {
        // In real implementation, this would use email service
        System.out.println("Sending email to: " + emailId);
        System.out.println("Subject: " + subject);
        System.out.println("Content: " + description);
        return true;
    }
    
    public void addAttachment(String attachmentPath) {
        attachments.add(attachmentPath);
    }
}

// SMS Notification implementation
public class SMSNotification extends Notification {
    private String phoneNumber;
    private int characterLimit = 160;
    
    public SMSNotification(String phoneNumber, String description, NotificationType type) {
        super(description, type);
        this.phoneNumber = phoneNumber;
    }
    
    @Override
    protected boolean validate() {
        // Validate phone number format and message length
        return phoneNumber != null && 
               phoneNumber.matches("^[+]?[0-9]{10,15}$") &&
               description.length() <= characterLimit;
    }
    
    @Override
    protected boolean sendNotification() {
        // In real implementation, this would use SMS gateway
        System.out.println("Sending SMS to: " + phoneNumber);
        System.out.println("Message: " + description);
        return true;
    }
}

// Status class for order tracking
public class Status {
    private String statusId;
    private Order order;  // Status belongs to an order
    private ShipmentStatus currentStatus;
    private Date lastUpdated;
    private Date expectedDeliveryDate;
    private String description;
    private List<StatusHistory> statusHistory;
    private String trackingNumber;
    private String courierPartner;
    
    public Status(Order order) {
        this.statusId = generateStatusId();
        this.order = order;
        this.currentStatus = ShipmentStatus.ORDER_PLACED;
        this.lastUpdated = new Date();
        this.statusHistory = new ArrayList<>();
        this.trackingNumber = generateTrackingNumber();
        calculateExpectedDelivery();
        
        // Add initial status to history
        addStatusUpdate(ShipmentStatus.ORDER_PLACED, "Order has been placed successfully");
    }
    
    public void updateStatus(ShipmentStatus newStatus, String description) {
        this.currentStatus = newStatus;
        this.description = description;
        this.lastUpdated = new Date();
        
        addStatusUpdate(newStatus, description);
        
        // Send notification for important status changes
        if (shouldNotifyUser(newStatus)) {
            sendStatusNotification();
        }
    }
    
    private void addStatusUpdate(ShipmentStatus status, String description) {
        StatusHistory history = new StatusHistory(status, description, new Date());
        statusHistory.add(history);
    }
    
    private boolean shouldNotifyUser(ShipmentStatus status) {
        return status == ShipmentStatus.SHIPPED || 
               status == ShipmentStatus.OUT_FOR_DELIVERY ||
               status == ShipmentStatus.DELIVERED ||
               status == ShipmentStatus.DELIVERY_FAILED;
    }
    
    private void sendStatusNotification() {
        // Create and send notification
        String message = String.format("Your order %s is %s", 
            order.getOrderId(), currentStatus.getDescription());
        
        Notification notification = new EmailNotification(
            order.getUser().getEmail(),
            "Order Status Update",
            message,
            NotificationType.SHIPMENT_UPDATE
        );
        
        notification.send();
    }
    
    private void calculateExpectedDelivery() {
        // Simple calculation - 3-5 business days
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, 5);
        this.expectedDeliveryDate = cal.getTime();
    }
}

// Status History for tracking all status changes
class StatusHistory {
    private ShipmentStatus status;
    private String description;
    private Date timestamp;
    private String location;
    
    public StatusHistory(ShipmentStatus status, String description, Date timestamp) {
        this.status = status;
        this.description = description;
        this.timestamp = timestamp;
    }
}

// Search Interface
public interface SearchInterface {
    List<Product> search(String query);
    List<Product> searchByCategory(String category);
    List<Product> searchByPriceRange(double minPrice, double maxPrice);
    List<Product> searchWithFilters(SearchCriteria criteria);
}

// Search Implementation
public class ProductSearchService implements SearchInterface {
    private ProductCatalog catalog;
    
    public ProductSearchService() {
        this.catalog = ProductCatalog.getInstance();
    }
    
    @Override
    public List<Product> search(String query) {
        return catalog.searchProducts(query);
    }
    
    @Override
    public List<Product> searchByCategory(String category) {
        return catalog.getProductsByCategory(category);
    }
    
    @Override
    public List<Product> searchByPriceRange(double minPrice, double maxPrice) {
        return catalog.getAllProducts().stream()
            .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Product> searchWithFilters(SearchCriteria criteria) {
        // Complex search with multiple filters
        Stream<Product> stream = catalog.getAllProducts().stream();
        
        if (criteria.getKeyword() != null) {
            stream = stream.filter(p -> p.getName().contains(criteria.getKeyword()));
        }
        
        if (criteria.getMinPrice() > 0) {
            stream = stream.filter(p -> p.getPrice() >= criteria.getMinPrice());
        }
        
        if (criteria.getMaxPrice() > 0) {
            stream = stream.filter(p -> p.getPrice() <= criteria.getMaxPrice());
        }
        
        if (criteria.getCategory() != null) {
            stream = stream.filter(p -> p.getCategory().getName().equals(criteria.getCategory()));
        }
        
        return stream.collect(Collectors.toList());
    }
}

// Supporting Enums
enum AccountStatus {
    ACTIVE, BLOCKED, SUSPENDED
}

enum OrderStatus {
    PENDING_PAYMENT, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED, PAYMENT_FAILED
}

enum PaymentStatus {
    PENDING, SUCCESS, FAILED, REFUNDED
}

enum ShipmentStatus {
    ORDER_PLACED("Order has been placed"),
    PROCESSING("Order is being processed"),
    SHIPPED("Order has been shipped"),
    IN_TRANSIT("Order is in transit"),
    OUT_FOR_DELIVERY("Order is out for delivery"),
    DELIVERED("Order has been delivered"),
    DELIVERY_FAILED("Delivery attempt failed"),
    RETURNED("Order has been returned");
    
    private String description;
    
    ShipmentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

enum NotificationType {
    ORDER_PLACED, PAYMENT_CONFIRMATION, SHIPMENT_UPDATE, DELIVERY, RETURN_INITIATED
}