package by.losik.errorfreetext.mapper;

import by.losik.errorfreetext.dto.TaskDto;
import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.Language;
import by.losik.errorfreetext.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class TaskMapperTest {

    @InjectMocks
    private TaskMapperImpl taskMapper;

    @Test
    public void shouldMapCreateRequestToEntity() {
        TaskDto.CreateRequest request = new TaskDto.CreateRequest();
        request.setText("Hello");
        request.setLanguage(Language.EN);

        CorrectionTask entity = taskMapper.toEntity(request);

        assertThat(entity.getOriginalText()).isEqualTo("Hello");
        assertThat(entity.getLanguage()).isEqualTo(Language.EN);
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.NEW);
    }
}