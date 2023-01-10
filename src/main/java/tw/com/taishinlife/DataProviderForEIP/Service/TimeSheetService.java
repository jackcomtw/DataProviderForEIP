package tw.com.taishinlife.DataProviderForEIP.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TimeSheetService {
    static Map<String,Map<String,String> >cookiesCache = new HashMap<String,Map<String,String>>();
    final String agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    final String url = "http://potsap01/TPISN/Month/TimeSheet.aspx";

    @Value("${us}")
    private String user;
    @Value("${ps}")
    private String password;

    @Autowired
    private ProcessService processService;

    final SimpleDateFormat dataTimeFormat  = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    final SimpleDateFormat dataFormat  = new SimpleDateFormat("yyyy/MM/dd");

    //秒 分 時 日 月 周
    //@Scheduled(cron="0 0/3 8,9,18,19 ? * MON-SAT")
    //@Scheduled(cron="* * * * * *")
    public void main() throws ParseException {
        Assert.notNull(user,"沒有帳號");
        Assert.notNull(password,"沒有密碼");
        log.info("Hi~~~ user:{},pw:{}",user,password);
        List<String> list = getCheckTime();
        Calendar currentTime = Calendar.getInstance();
        currentTime.setTime(dataTimeFormat.parse(list.get(0)));
        if (checkWorkDay(currentTime)){
            if (checkCheckTime(list)) {
                waitTime();
                log.info("時間到了，準備打卡");
                setCheckTime();
            }
        }
        log.info("GoodBye");
    }

    //秒 分 時 日 月 周
    @Scheduled(cron="0 0 8,17 * * ?")
    public void update(){
        processService.runCommand(
                new ProcessBuilder().command(
                        "cmd.exe", "/c",
                        "git fetch --all && git checkout origin/master -- holiday.json" )
        );
    }


    @SneakyThrows
    public List<String> getCheckTime() {
        Document doc = connection(100000, getHeader());
        List<String> results = new ArrayList<String>(){{
            add(StringUtils.defaultString(doc.getElementById("current_time").text(),""));  //2022/12/29 13:52:06
            add(StringUtils.defaultString(doc.getElementById("check_in_time").text(),""));  //2022/12/29 08:57:52
            add(StringUtils.defaultString(doc.getElementById("check_out_time").text(),"")); //2022/12/29 18:02:38
        }};
        return results;
    }

    @SneakyThrows
    public boolean  checkCheckTime(List<String> times) {
        log.info("checkCheckTime begin");
        Calendar currentTime = null,
                checkInTime = null,
                checkOutTime = null;

        if (times.size() != 3) {
            log.info("checkCheckTime end return false");
            return false;
        }
        if (StringUtils.isNotBlank(times.get(0))){
            currentTime = Calendar.getInstance();
            currentTime.setTime(dataTimeFormat.parse(times.get(0)));
        }
        if (StringUtils.isNotBlank(times.get(1))){
            checkInTime = Calendar.getInstance();
            checkInTime.setTime(dataTimeFormat.parse(times.get(1)));
        }
        if (StringUtils.isNotBlank(times.get(2))){
            checkOutTime = Calendar.getInstance();
            checkOutTime.setTime(dataTimeFormat.parse(times.get(2)));
        }

        log.info("現在時間:{}, 上班打卡時間:{} , 下班打卡時間:{}",
                currentTime==null?"":dataTimeFormat.format(currentTime.getTime()),
                checkInTime==null?"":dataTimeFormat.format(checkInTime.getTime()),
                checkOutTime==null?"":dataTimeFormat.format(checkOutTime.getTime()));

        Calendar checkInTime_Begin = Calendar.getInstance(),
                checkInTime_End = Calendar.getInstance(),
                checkOutTime_Begin = Calendar.getInstance(),
                checkOutTime_End = Calendar.getInstance();
        //08:50~09:59
        checkInTime_Begin.set(Calendar.HOUR_OF_DAY,8);
        checkInTime_Begin.set(Calendar.MINUTE,50);
        checkInTime_Begin.set(Calendar.SECOND ,00);
        checkInTime_End.set(Calendar.HOUR_OF_DAY,9);
        checkInTime_End.set(Calendar.MINUTE,59);
        checkInTime_End.set(Calendar.SECOND ,00);

        //checkInTime+9hr~19:00
        if (checkInTime != null && checkInTime.get(Calendar.HOUR_OF_DAY) >= 9) {
            //早上九點以後打卡
            checkOutTime_Begin = checkInTime;
            checkOutTime_Begin.add(Calendar.HOUR_OF_DAY,9);
            checkOutTime_Begin.add(Calendar.MINUTE,5);
        }else {
            //早上九點之前打卡
            checkOutTime_Begin.set(Calendar.HOUR_OF_DAY, 18);
            checkOutTime_Begin.set(Calendar.MINUTE, 00);
            checkOutTime_Begin.set(Calendar.SECOND, 00);
        }
        checkOutTime_End.set(Calendar.HOUR_OF_DAY,19);
        checkOutTime_End.set(Calendar.MINUTE,00);
        checkOutTime_End.set(Calendar.SECOND ,00);

        if (currentTime.after(checkInTime_Begin) && currentTime.before(checkInTime_End)) {
            //上午(08:00~09:59)
            if (checkInTime == null) {
                //還沒打卡
                log.info("checkCheckTime end return true");
                return true;
            }
        }else if (currentTime.after(checkOutTime_Begin) && currentTime.before(checkOutTime_End)){
            //下午(18:00~19:00)
            if (checkOutTime == null) {
                //還沒打卡
                log.info("checkCheckTime end return true");
                return true;
            }
        }
        log.info("checkCheckTime end return false");
        return false;
    }

    @SneakyThrows
    public boolean  setCheckTime() {
        log.info("*****************setCheckTime begin*******************");
        URI uri = new URI(getUrl());
        Map<String,String> header = getHeader();
        Map<String,String> cookies = cookiesCache.get(uri.getHost());
        header.put("Content-Length","0");
        header.put("Origin","http://potsap01");
        header.put("Referer","http://potsap01/TPISN/Month/TimeSheet.aspx");
        header.put("X-Requested-With", "XMLHttpRequest");

        Connection.Response respance =  Jsoup.connect("http://potsap01/TPISN/Handlers/CheckInSheet.ashx")
                .ignoreContentType(true)
                .method(Connection.Method.POST)
                .headers(header)
                .timeout(100000)
                .cookies(cookies)
                .execute();

        log.info("setCheckTime respance: {},{}",respance.statusCode(), respance.body());

        return respance.statusCode()==200?true:false;
    }

    @SneakyThrows
    public String getUrl() {
        final Matcher matcher = Pattern.compile("^(http[s]?\\:\\/\\/)(.+)$").matcher(url);
        if (!matcher.find() && matcher.groupCount() != 2) {
            throw new RuntimeException("url parser error");
        }else {
            return matcher.group(1) + user + ":" + password + "@" + matcher.group(2);
        }
    }

    private void waitTime() {
        //休息33~120秒
        int sec = RandomUtils.nextInt(33,120);

        log.info("{} will sleep {} sec", Thread.currentThread().getName() , sec);
        long nextFreezeEnd = System.currentTimeMillis() + (sec * 1000);
        while (System.currentTimeMillis() < nextFreezeEnd) {
            try {
                Thread.sleep(1000);
            }catch (Exception e) {
                log.error("Thread.sleep",e);
            }
        }
        return;
    }

    private Document connection(int timeout, Map<String, String> header) throws NoSuchAlgorithmException, NoSuchProviderException, KeyManagementException, IOException, URISyntaxException {
        URI uri = new URI(getUrl());
        Map<String,String> cookies = cookiesCache.get(uri.getHost());
        if (cookies == null) {
            Connection.Response resp =
                    Jsoup.connect(uri.toString())
                            .userAgent(agent)
                            .headers(header)
                            .timeout(timeout)
                            .execute();
            cookies = resp.cookies();
            cookiesCache.put(uri.getHost(),cookies);
        }
        return Jsoup.connect(uri.toString())
                .userAgent(agent)
                .headers(header)
                .timeout(timeout)
                .cookies(cookies)
                .get();

    }

    private Map<String, String> getHeader() {
        return new HashMap<String, String>() {{
            put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            put("Accept-Language", "en-US,en;q=0.9");
            put("Cache-Control", "no-cache");
            put("Connection","keep-alive");
            put("Pragma","no-cache");
            put("Upgrade-Insecure-Requests","1");
        }};
    }

    @SneakyThrows
    public boolean checkWorkDay(final Calendar today) {
        String directory = Paths.get(".").toAbsolutePath().normalize().toString();
        directory = directory +(StringUtils.defaultString(StringUtils.right(directory, 1)).equals(File.separator)?"":File.separator);
        File file = new File(directory,"holiday.json");
        if (!file.exists() || !file.isFile()){
            log.error("查無{}檔案", file.toString());
            throw new FileNotFoundException(file.toString());
        }

        ObjectMapper mapper = new ObjectMapper();
        List<String> holidays = mapper.readValue(file, new TypeReference<ArrayList<String>>(){});

        long count = holidays.stream()
                .filter(
                    x-> Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$").matcher(x).matches()
                ).filter(
                    x-> {
                        try {
                            Calendar day = Calendar.getInstance();
                            day.setTime(dataFormat.parse(x));
                            boolean matches = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                                    day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) ? true: false;
                            if (matches) {
                                log.info("{}吻合holiday，今天不用打卡", x);
                            }
                            return matches;
                        } catch (ParseException e) {
                            log.error("checkWorkDay error", e);
                            return false;
                        }
                    }
                ).count();

        if (count==0){
            log.info("{}要打卡",dataFormat.format(today.getTime()));
        }
        return count==0?true:false;


    }
}
