package tw.com.taishinlife.DataProviderForEIP.Dao.Impl;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import tw.com.taishinlife.DataProviderForEIP.Dto.ProjectConfigDto;

import java.util.List;

@Mapper
public interface ProjectConfigDao  {
    @Select("SELECT * FROM dbo.PROJECT_CONFIG")
    public List<ProjectConfigDto> findAll();

}

