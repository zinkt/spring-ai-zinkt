package top.zinkt.springaizinkt.data;

public class FlightPrice {
    private String from;
    private String to;
    private BookingClass bookingClass;
    private int price; // 单位：RMB

    public FlightPrice(String from, String to, BookingClass bookingClass, int price) {
        this.from = from;
        this.to = to;
        this.bookingClass = bookingClass;
        this.price = price;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BookingClass getBookingClass() {
        return bookingClass;
    }

    public void setBookingClass(BookingClass bookingClass) {
        this.bookingClass = bookingClass;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
