package tw.com.taishinlife.DataProviderForEIP;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tw.com.taishinlife.DataProviderForEIP.Service.TimeSheetService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

@SpringBootTest
class DataProviderForEipApplicationTests {


	@Autowired
	private TimeSheetService timeSheetService;

	void testSetCheckTime(){
		System.out.println(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()));
		//timeSheetService.setCheckTime();
		System.out.println(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()));
	}


	void test() {
		List<String> lists = timeSheetService.getCheckTime();
		Assertions.assertEquals(lists.size(),3);

		String dateStr = StringUtils.substringBefore(lists.get(0)," ");
		Assertions.assertTrue(Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$").matcher(dateStr).matches());

		//上班沒有打卡
		lists.set(0,dateStr+" 07:59:00"); //currentTime
		lists.set(1,""); //checkinTime
		lists.set(2,""); //checkOutTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 08:50:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 08:55:00"); //currentTime
		Assertions.assertTrue(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 09:20:00"); //currentTime
		Assertions.assertTrue(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 10:00:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));

		//上班有打卡
		lists.set(1,dateStr+" 08:59:10"); //checkinTime
		lists.set(0,dateStr+" 08:50:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 08:55:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 09:20:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 10:00:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));

		//下班沒有打卡
		lists.set(0,dateStr+" 07:59:00"); //currentTime
		lists.set(1,""); //checkinTime
		lists.set(2,""); //checkOutTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 13:50:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 18:00:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 18:00:01"); //currentTime
		Assertions.assertTrue(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 18:59:00"); //currentTime
		Assertions.assertTrue(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 19:00:00"); //currentTime
		Assertions.assertTrue(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 19:00:01"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));

		//下班有打卡
		lists.set(0,dateStr+" 07:59:00"); //currentTime
		lists.set(1,""); //checkinTime
		lists.set(2,dateStr+" 18:05:10"); //checkOutTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 13:50:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 18:00:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 18:00:01"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 18:59:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 19:00:00"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
		lists.set(0,dateStr+" 19:00:01"); //currentTime
		Assertions.assertFalse(timeSheetService.checkCheckTime(lists));
	}

	@Test()
	void testURL() {
		String url = timeSheetService.getUrl();
		Assertions.assertEquals(url, "http://T777019:!QAZ2wsx2@potsap01/TPISN/Month/TimeSheet.aspx");
	}

	@Test
	@SneakyThrows
	void testCheckWorkDay() {
		Calendar calender = Calendar.getInstance();
		calender.setTime(new SimpleDateFormat("yyyy/MM/dd").parse("2023/01/01"));
		int count = 0;
		while (calender.get(Calendar.YEAR) <2024) {
			if (!timeSheetService.checkWorkDay(calender)) {
				count ++;
			}
			calender.add(Calendar.DAY_OF_YEAR,1);
		}
		Assertions.assertEquals(count,116);
	}
}
