package tw.com.taishinlife.DataProviderForEIP.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectConfigDto {
    @Id
    int id;
    String projectName;
    String modifyUser;
    String createTime;
}
