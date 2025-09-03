package top.zinkt.springaizinkt.services;

import top.zinkt.springaizinkt.data.Booking;
import top.zinkt.springaizinkt.data.Customer;
import top.zinkt.springaizinkt.data.FlightPrice;

import java.util.ArrayList;
import java.util.List;

public class BookingData {

	private List<Customer> customers = new ArrayList<>();
	private List<Booking> bookings = new ArrayList<>();
	private List<FlightPrice> prices = new ArrayList<>();

	// ---- Customers ----
	public List<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(List<Customer> customers) {
		this.customers = customers;
	}

	// ---- Bookings ----
	public List<Booking> getBookings() {
		return bookings;
	}

	public void setBookings(List<Booking> bookings) {
		this.bookings = bookings;
	}

	// ---- Prices ----
	public List<FlightPrice> getPrices() {
		return prices;
	}

	public void setPrices(List<FlightPrice> prices) {
		this.prices = prices;
	}
}
