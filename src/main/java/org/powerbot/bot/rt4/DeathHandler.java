package org.powerbot.bot.rt4;

import org.powerbot.bot.rt4.client.internal.IBasicObject;
import org.powerbot.bot.rt4.client.internal.IGameObject;
import org.powerbot.bot.rt4.client.internal.INpc;
import org.powerbot.bot.rt4.client.internal.IWidget;
import org.powerbot.script.*;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.*;

import java.util.logging.Logger;

import static org.powerbot.script.rt4.Constants.DEATH_CHAT_WIDGET_COMPONENT_ID;
import static org.powerbot.script.rt4.Constants.DEATH_CHAT_WIDGET_ID;

public class DeathHandler extends Daemon<ClientContext> {

	private final Logger logger = Logger.getLogger("DeathHandler");

	private boolean canLeave = true;

	public DeathHandler(ClientContext ctx) {
		super(ctx);
	}

	@Override
	public String name() {
		return "DeathHandler";
	}

	@Override
	public String description() {
		return "Gets you back to the mainland.";
	}

	@Override
	public boolean check() {
		return getDeath() != Npcs.NIL;
	}

	private INpc getDeath() {
		return ctx.npcs.toStream().within(10).name("Death").first();
	}

	private IWidget getChatComponent() {
		return ctx.widgets.component(DEATH_CHAT_WIDGET_ID, DEATH_CHAT_WIDGET_COMPONENT_ID);
	}

	private IWidget getNextChatOptionComponent() {
		IWidget component = getChatComponent();
		if (component.valid()) {
			for (IWidget child : component.components()) {
				if (child.text().startsWith("<str>") || child.text() == null || child.text().equals("") || child.text().contains("Select an Option")) {
					continue;
				}
				return child;
			}
		}

		return Widgets.NIL;
	}

	@Override
	public boolean execute() {
		if (canLeave) {
			IBasicObject portal = ctx.objects.toStream().within(10).name("Portal").first();
			if (portal == Objects.NIL) {
				logger.severe("Failed to find death portal");
				return false;
			}

			if (!portal.interact("Use")) {
				ctx.movement.moveTo(portal, false, false);
				return false;
			}

			Condition.wait(() -> ctx.objects.toStream().within(10).name("Portal").first() == Objects.NIL ||
				ctx.chat.canContinue());
			if (ctx.chat.canContinue()) {
				canLeave = false;
			}
			return false;
		}

		if (ctx.chat.canContinue()) {
			ctx.chat.clickContinue();
			return false;
		}

		IWidget next = getNextChatOptionComponent();
		if (!next.isNil()) {
			if (next.text().contains("I think I'm done")) {
				canLeave = true;
			}

			next.click(true);
			Condition.wait(ctx.chat::canContinue);
			return false;
		}

		INpc death = getDeath();
		if (death.valid() && !ctx.players.local().interacting().equals(death)) {
			if (!death.interact("Talk-to")) {
				ctx.movement.moveTo(death, false, false);
			}
			Condition.wait(ctx.chat::canContinue);
			return false;
		}

		return false;
	}
}
