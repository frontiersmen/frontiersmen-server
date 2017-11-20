package me.ericjiang.settlers.library;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public abstract class Game extends MultiplayerModule {

    private final String name;

    public abstract GameSummary summarize();

}
