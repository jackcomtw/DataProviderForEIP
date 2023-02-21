package tw.com.taishinlife.DataProviderForEIP.Service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

@Service
@Slf4j
public class ProcessService {
    @SneakyThrows
    public Boolean runCommand(ProcessBuilder processBuilder){
        return runCommand(processBuilder, Charset.forName("BIG5"));
    }

    @SneakyThrows
    private Boolean runCommand(ProcessBuilder processBuilder, Charset charset){
        String command =
                Optional.ofNullable(processBuilder.command())
                        .orElse(Arrays.asList(""))
                        .stream()
                        .filter(x->!x.equals("cmd.exe") && !x.equals("/c"))
                        .collect(Collectors.joining());

        log.info("執行指令 : {}",command);
        log.warn("update holiday",command);

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream(), charset));
        List<String> lines = reader.lines().map(line -> chomp(line)).collect(Collectors.toList());
        List<String> errorLines = error.lines().map(line -> chomp(line)).collect(Collectors.toList());

        String errorStr =
                Optional.ofNullable(errorLines)
                        .orElse(Arrays.asList(""))
                        .stream()
                        .filter(x->isNotBlank(x))
                        .collect(Collectors.joining());
        if (isBlank(errorStr)) {
            return true;
        }

        log.info("執行指令失敗，因為 " + errorStr);
        throw new Exception("執行指令失敗 : "+ command + ", 因為 " + errorStr);

    }
}
