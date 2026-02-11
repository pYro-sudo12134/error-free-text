package by.losik.errorfreetext.external.yandex.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellResult {
    private Integer code;
    private Integer pos;
    private Integer row;
    private Integer col;
    private Integer len;
    private String word;

    @JsonProperty("s")
    private List<String> suggestions;
}