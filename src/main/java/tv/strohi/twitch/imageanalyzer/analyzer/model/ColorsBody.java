package tv.strohi.twitch.imageanalyzer.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ColorsBody {
    private int[] ownTeamColor;
    private int[] otherTeamColor;
}
