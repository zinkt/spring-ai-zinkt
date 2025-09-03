package top.zinkt.springaizinkt.services;


import top.zinkt.springaizinkt.data.*;
import org.springframework.stereotype.Service;
import top.zinkt.springaizinkt.data.Booking;
import top.zinkt.springaizinkt.data.BookingClass;
import top.zinkt.springaizinkt.data.BookingStatus;
import top.zinkt.springaizinkt.data.Customer;
import top.zinkt.springaizinkt.services.BookingTools.BookingDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class FlightBookingService {

	private final BookingData db;

	public FlightBookingService() {
		db = new BookingData();
		initDemoData();
	}

	private void initDemoData() {
		List<String> names = List.of("赵颖", "陈静", "王红文", "陈笑", "黄浩然", "David Jones", "庄周", "吴建国", "Rick Sanchez", "Jessica Davis");
		List<String> airportCodes = List.of(
				"PEK/BJS 北京首都国际机场", "KIX 关西国际机场", "CTU 成都双流国际机场", "TFU 成都天府国际机场", "CAN 广州白云国际机场", "NKG 南京禄口国际机场",
				"LHR 伦敦希思罗机场", "PVG 上海浦东国际机场", "LAX 洛杉矶国际机场", "HKG 香港国际机场", "HGH 杭州萧山国际机场", "PKX 北京大兴国际机场", "TSA 台北松山机场"
		);
		Random random = new Random();

		var customers = new ArrayList<Customer>();
		var bookings = new ArrayList<Booking>();
		var prices = new ArrayList<FlightPrice>();

		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);

			// 确保出发地 != 目的地
			String from, to;
			do {
				from = airportCodes.get(random.nextInt(airportCodes.size()));
				to = airportCodes.get(random.nextInt(airportCodes.size()));
			} while (from.equals(to));

			BookingClass bookingClass = BookingClass.values()[random.nextInt(BookingClass.values().length)];
			Customer customer = new Customer();
			customer.setName(name);

			// 日期：未来 7 ~ 30 天之间随机
			LocalDate date = LocalDate.now().plusDays(7 + random.nextInt(24));

			// 生成更真实的预订号（字母+数字）
			String bookingNumber = "BK" + (1000 + random.nextInt(9000));

			Booking booking = new Booking(
					bookingNumber,
					date,
					customer,
					BookingStatus.CONFIRMED,
					from,
					to,
					bookingClass
			);
			customer.getBookings().add(booking);

			// 添加到集合
			customers.add(customer);
			bookings.add(booking);

			// 生成航班价格
			FlightPrice flightPrice = getFlightPrice(from, to, bookingClass);
			prices.add(flightPrice);
		}

		db.setCustomers(customers);
		db.setBookings(bookings);
		db.setPrices(prices);
	}

	// 获取所有已预订航班
	public List<BookingDetails> getBookings() {
		return db.getBookings().stream().map(this::toBookingDetails).toList();
	}

	// 支持 bookingNumber 或 name
	private Booking findBooking(String bookingNumber, String name) {
		return db.getBookings()
				.stream()
				.filter(b ->
						(bookingNumber != null && !bookingNumber.isBlank() &&
								b.getBookingNumber().equalsIgnoreCase(bookingNumber))
								||
								(name != null && !name.isBlank() &&
										b.getCustomer().getName().equalsIgnoreCase(name))
				)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Booking not found for bookingNumber=" + bookingNumber + ", name=" + name
				));
	}

	// 允许单条件查询
	public BookingDetails getBookingDetails(String bookingNumber, String name) {
		var booking = findBooking(bookingNumber, name);
		return toBookingDetails(booking);
	}


	// 更改预定航班
	public void changeBooking(String bookingNumber, String name, String newDate, String from, String to) {
		var booking = findBooking(bookingNumber, name);
		if (booking.getDate().isBefore(LocalDate.now().plusDays(1))) {
			throw new IllegalArgumentException("Booking cannot be changed within 24 hours of the start date.");
		}
		booking.setDate(LocalDate.parse(newDate));
		booking.setFrom(from);
		booking.setTo(to);
	}

	// 取消预定航班
	public void cancelBooking(String bookingNumber, String name) {
		var booking = findBooking(bookingNumber, name);
//		// 是不是发车前2天
//		if (booking.getDate().isBefore(LocalDate.now().plusDays(2))) {
//			throw new IllegalArgumentException("Booking cannot be cancelled within 48 hours of the start date.");
//		}
		booking.setBookingStatus(BookingStatus.CANCELLED);
	}

	private BookingDetails toBookingDetails(Booking booking) {
		return new BookingDetails(booking.getBookingNumber(), booking.getCustomer().getName(), booking.getDate(),
				booking.getBookingStatus(), booking.getFrom(), booking.getTo(), booking.getBookingClass().toString());
	}

	//todo 预定航班
	public BookingDetails bookFlight(String name, String from, String to, String date, BookingClass bookingClass) {
		Random random = new Random();
		// 1. 检查用户是否存在，不存在则新建
		Customer customer = db.getCustomers().stream()
				.filter(c -> c.getName().equalsIgnoreCase(name))
				.findFirst()
				.orElseGet(() -> {
					Customer newCustomer = new Customer();
					newCustomer.setName(name);
					db.getCustomers().add(newCustomer);
					return newCustomer;
				});

		// 2. 生成预定编号（简单起见，随机数+时间戳）
		String bookingNumber = "BN" + (1000 + random.nextInt(9000));

		// 3. 构建预定对象
		LocalDate localDate = LocalDate.parse(date);
		Booking booking = new Booking(
				bookingNumber,
				localDate,
				customer,
				BookingStatus.CONFIRMED,
				from,
				to,
				bookingClass
		);

		// 4. 保存预定
		customer.getBookings().add(booking);
		db.getBookings().add(booking);

		// 5. 返回详情
		return toBookingDetails(booking);
	}

	// todo 获取航班价格
	public FlightPrice getFlightPrice(String from, String to, BookingClass bookingClass) {
		// 模拟计算价格
		int basePrice = 500; // 起步价
		int distanceFactor = Math.abs(from.hashCode() - to.hashCode()) % 300; // 随机模拟
		int classFactor = switch (bookingClass) {
			case ECONOMY -> 1;
			case SUPER_ECONOMY -> 2;
			case BUSINESS -> 3;
			case FIRST -> 5;
		};

		int price = basePrice + distanceFactor * classFactor;

		return new FlightPrice(from, to, bookingClass, price);
	}


}
