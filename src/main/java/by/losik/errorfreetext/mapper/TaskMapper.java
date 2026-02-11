package by.losik.errorfreetext.mapper;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.TaskStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "originalText", source = "text")
    @Mapping(target = "correctedText", ignore = true)
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    CorrectionTask toEntity(TaskDto.CreateRequest request);

    default TaskDto.CreateResponse toCreateResponse(CorrectionTask task) {
        if (task == null) {
            return null;
        }
        return TaskDto.CreateResponse.builder()
                .taskId(task.getId())
                .build();
    }

    @Mapping(source = "id", target = "taskId")
    @Mapping(target = "originalText", ignore = true)
    @Mapping(source = "correctedText", target = "correctedText")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "language", target = "language")
    @Mapping(source = "errorMessage", target = "errorMessage")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "processedAt", target = "processedAt")
    TaskDto.GetResponse toGetResponse(CorrectionTask task);

    List<TaskDto.GetResponse> toGetResponseList(List<CorrectionTask> tasks);

    @AfterMapping
    default void setStatus(@MappingTarget CorrectionTask.CorrectionTaskBuilder builder) {
        builder.status(TaskStatus.NEW);
    }
}