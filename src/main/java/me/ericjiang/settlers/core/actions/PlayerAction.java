package me.ericjiang.settlers.core.actions;

import lombok.Getter;
import lombok.Setter;
import me.ericjiang.settlers.core.game.Game;

@Getter
@Setter
public abstract class PlayerAction extends Action {

    private String playerId;

    public abstract void accept(Game game);

}