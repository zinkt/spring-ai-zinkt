package top.zinkt.springaizinkt.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import top.zinkt.springaizinkt.data.BookingClass;
import top.zinkt.springaizinkt.data.BookingStatus;
import top.zinkt.springaizinkt.data.FlightPrice;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class BookingTools {

	@Autowired
	FlightBookingService  flightBookingService;

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, LocalDate date, BookingStatus bookingStatus,
			String from, String to, String bookingClass) {
	}

	public record CancelBookingRequest(@ToolParam(description = "预定号，以BK开头") String bookingNumber, @ToolParam(description = "客户姓名") String name) {}
	@Bean
	@Tool(description = "处理机票退订")
	public Function<CancelBookingRequest, String> cancelBookingRequestTool(){
		return cancelBookingRequest -> {
			// 调用退订业务方法
            try {
                flightBookingService.cancelBooking(cancelBookingRequest.bookingNumber, cancelBookingRequest.name);
				return "退订成功";
            } catch (Exception e) {
				return "退订失败，未查询到对应的预定单";
            }
		};
	}

	// 请求对象：姓名和预定号都可选
	public record BookingDetailsRequest(String bookingNumber, String name) {}

	@Bean
	@Tool(description = "获取机票预定信息，查询机票")
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetailsTools() {
		return bookingDetailsRequest -> {
			try {
				return flightBookingService.getBookingDetails(
						bookingDetailsRequest.bookingNumber(),
						bookingDetailsRequest.name()
				);
			} catch (Exception e) {
				// 查不到时返回一个空壳，避免 tool call 报错中断
				return new BookingDetails(
						bookingDetailsRequest.bookingNumber(),
						bookingDetailsRequest.name(),
						null, null, null, null, null
				);
			}
		};
	}


	// ----------- 改签航班 -----------
	public record ChangeBookingRequest(
			@ToolParam(description = "预定号") String bookingNumber,
			@ToolParam(description = "客户姓名") String name,
			@ToolParam(description = "新的出发日期，格式: yyyy-MM-dd") String newDate,
			@ToolParam(description = "新的出发地") String from,
			@ToolParam(description = "新的目的地") String to
	) {}

	@Bean
	@Tool(description = "处理机票改签")
	public Function<ChangeBookingRequest, String> changeBookingRequestTool() {
		return changeBookingRequest -> {
			flightBookingService.changeBooking(
					changeBookingRequest.bookingNumber,
					changeBookingRequest.name,
					changeBookingRequest.newDate,
					changeBookingRequest.from,
					changeBookingRequest.to
			);
			return "改签成功";
		};
	}

	// ----------- 预定航班 -----------
	public record BookFlightRequest(
			@ToolParam(description = "客户姓名") String name,
			@ToolParam(description = "出发地") String from,
			@ToolParam(description = "目的地") String to,
			@ToolParam(description = "出发日期，格式: yyyy-MM-dd") String date,
			@ToolParam(description = "舱位等级，如 ECONOMY、SUPER_ECONOMY、BUSINESS、FIRST") BookingClass bookingClass
	) {}
	@Bean
	@Tool(description = "处理机票预定")
	public Function<BookFlightRequest, BookingDetails> bookFlightRequestTool() {
		return bookFlightRequest -> flightBookingService.bookFlight(
				bookFlightRequest.name,
				bookFlightRequest.from,
				bookFlightRequest.to,
				bookFlightRequest.date,
				bookFlightRequest.bookingClass
		);
	}

	// ----------- 获取航班价格 -----------
	public record FlightPriceRequest(
			@ToolParam(description = "出发地") String from,
			@ToolParam(description = "目的地") String to,
			@ToolParam(description = "舱位等级，如 ECONOMY、SUPER_ECONOMY、BUSINESS、FIRST") BookingClass bookingClass
	) {}

	@Bean
	@Tool(description = "获取航班价格信息")
	public Function<FlightPriceRequest, FlightPrice> getFlightPriceRequestTool() {
		return flightPriceRequest -> flightBookingService.getFlightPrice(
				flightPriceRequest.from,
				flightPriceRequest.to,
				flightPriceRequest.bookingClass
		);
	}


//	// 记录类：Text-to-SQL 请求
//	public record TextToSqlRequest(
//			@ToolParam(description = "用户的自然语言查询，例如 '查询从北京到上海的机票'") String query
//	) {}

//	@Bean
//	@Tool(description = "将自然语言转换为 SQL 查询数据库，并返回机票相关结果。仅用于查询操作。")
//	public Function<TextToSqlRequest, String> textToSqlTool(
//			@Autowired JdbcTemplate jdbcTemplate, // 用于执行 SQL
//			@Autowired ChatModel chatModel, // 用于生成 SQL 的 LLM 模型
//			@Value("classpath:/templates/text-to-sql-default-prompt.st") Resource sqlPromptResource,
//			@Value("classpath:/templates/booking-schema.sql") Resource sqlSchema
//	) {
//		return request -> {
//			try {
//				// 步骤1: 使用 LLM 生成 SQL
//				String promptTemplate = sqlPromptResource.getContentAsString(Charset.defaultCharset()); // 读取提示模板
//				String fullPrompt = promptTemplate.replace("{schema}", sqlSchema.getContentAsString(Charset.defaultCharset()))
//						.replace("{query}", request.query());
//				String generatedSql = chatModel.call(fullPrompt); // 调用 LLM 生成 SQL
//
//				// 步骤2: 执行 SQL（假设只允许 SELECT）
//				if (!generatedSql.toUpperCase().startsWith("SELECT")) {
//					return "错误：仅支持查询操作。";
//				}
//				List<Map<String, Object>> results = jdbcTemplate.queryForList(generatedSql);
//
//				// 步骤3: SQL2Text - 将结果转换为友好文本
//				if (results.isEmpty()) {
//					return "未找到相关机票信息。";
//				}
//				String textResult = results.stream()
//						.map(row -> "预订号: " + row.get("booking_number") + ", 姓名: " + row.get("name") +
//								", 日期: " + row.get("date") + ", 状态: " + row.get("status") +
//								", 从: " + row.get("from_city") + " 到: " + row.get("to_city") +
//								", 舱位: " + row.get("booking_class"))
//						.collect(Collectors.joining("\n"));
//
//				return "查询结果：\n" + textResult;
//			} catch (Exception e) {
//				return "处理查询时出错：" + e.getMessage();
//			}
//		};
//	}


}
