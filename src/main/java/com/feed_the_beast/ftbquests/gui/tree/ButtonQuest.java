package com.feed_the_beast.ftbquests.gui.tree;

import com.feed_the_beast.ftblib.lib.gui.Button;
import com.feed_the_beast.ftblib.lib.gui.ContextMenuItem;
import com.feed_the_beast.ftblib.lib.gui.GuiHelper;
import com.feed_the_beast.ftblib.lib.gui.GuiIcons;
import com.feed_the_beast.ftblib.lib.gui.Panel;
import com.feed_the_beast.ftblib.lib.gui.Theme;
import com.feed_the_beast.ftblib.lib.gui.Widget;
import com.feed_the_beast.ftblib.lib.gui.WidgetType;
import com.feed_the_beast.ftblib.lib.icon.Color4I;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import com.feed_the_beast.ftblib.lib.util.misc.MouseButton;
import com.feed_the_beast.ftbquests.gui.FTBQuestsTheme;
import com.feed_the_beast.ftbquests.net.edit.MessageEditObjectDirect;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestObject;
import com.feed_the_beast.ftbquests.quest.reward.QuestReward;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author LatvianModder
 */
public class ButtonQuest extends Button
{
	public static final Color4I COL_COMPLETED = Color4I.rgb(0x56FF56);
	public static final Color4I COL_STARTED = Color4I.rgb(0x00FFFF);

	public GuiQuestTree treeGui;
	public Quest quest;
	public String description;
	public List<ButtonQuest> dependencies = null;

	public ButtonQuest(Panel panel, Quest q)
	{
		super(panel, q.getDisplayName().getFormattedText(), q.getIcon());
		treeGui = (GuiQuestTree) panel.getGui();
		setSize(20, 20);
		quest = q;
		description = TextFormatting.GRAY + StringUtils.unformatted(quest.description).replace("&(\\S)", StringUtils.FORMATTING_CHAR + "$1");

		if (StringUtils.unformatted(description).isEmpty())
		{
			description = "";
		}
	}

	@Override
	public boolean checkMouseOver(int mouseX, int mouseY)
	{
		if (treeGui.questLeft.isMouseOver() || treeGui.questRight.isMouseOver())
		{
			return false;
		}

		return super.checkMouseOver(mouseX, mouseY);
	}

	public List<ButtonQuest> getDependencies()
	{
		if (dependencies == null)
		{
			dependencies = new ArrayList<>();

			for (QuestObject object : quest.dependencies)
			{
				if (object instanceof Quest)
				{
					for (Widget widget : treeGui.quests.widgets)
					{
						if (widget instanceof ButtonQuest && object == ((ButtonQuest) widget).quest)
						{
							dependencies.add((ButtonQuest) widget);
						}
					}
				}
			}

			dependencies = dependencies.isEmpty() ? Collections.emptyList() : dependencies;
		}

		return dependencies;
	}

	@Override
	public void onClicked(MouseButton button)
	{
		GuiHelper.playClickSound();

		if (treeGui.questFile.canEdit() && button.isRight())
		{
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.move"), GuiIcons.UP, () -> {
				treeGui.movingQuest = true;
				treeGui.selectQuest(quest);

			}));

			if (treeGui.selectedQuest != null && treeGui.selectedQuest != quest)
			{
				if (treeGui.selectedQuest.hasDependency(quest))
				{
					contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.rem_dep"), GuiIcons.REMOVE, () -> editDependency(treeGui.selectedQuest, quest, false)));
				}
				else if (quest.hasDependency(treeGui.selectedQuest))
				{
					contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.rem_dep"), GuiIcons.REMOVE, () -> editDependency(quest, treeGui.selectedQuest, false)));
				}
				else
				{
					contextMenu.add(new ContextMenuItem(I18n.format("ftbquests.gui.set_dep"), FTBQuestsTheme.ADD, () -> editDependency(quest, treeGui.selectedQuest, true)).setEnabled(treeGui.selectedQuest != null && treeGui.selectedQuest != quest && !treeGui.selectedQuest.canRepeat));
				}
			}

			contextMenu.add(ContextMenuItem.SEPARATOR);
			GuiQuestTree.addObjectMenuItems(contextMenu, getGui(), quest);
			getGui().openContextMenu(contextMenu);
		}
		else if (button.isLeft())
		{
			if (treeGui.movingQuest && treeGui.selectedQuest == quest)
			{
				treeGui.movingQuest = false;
				treeGui.selectedQuest = null;
				treeGui.selectQuest(quest);
			}
			else
			{
				treeGui.open(quest);
			}
		}
		else if (treeGui.questFile.canEdit() && button.isMiddle())
		{
			treeGui.movingQuest = true;
			treeGui.selectQuest(quest);
		}
	}

	private void editDependency(Quest quest, QuestObject object, boolean add)
	{
		if (add ? quest.dependencies.add(object) : quest.dependencies.remove(object))
		{
			quest.verifyDependencies();
			new MessageEditObjectDirect(quest).sendToServer();
			treeGui.quests.refreshWidgets();
		}
	}

	@Override
	public void addMouseOverText(List<String> list)
	{
		String title = getTitle();

		if (treeGui.questFile.self != null)
		{
			int p = quest.getRelativeProgress(treeGui.questFile.self);

			if (p > 0 && p < 100)
			{
				title += " " + TextFormatting.DARK_GRAY + p + "%";
			}
		}

		list.add(title);

		if (!description.isEmpty())
		{
			list.add(description);
		}

		if (treeGui.questFile.self != null && quest.isComplete(treeGui.questFile.self))
		{
			int r = 0;

			for (QuestReward reward : quest.rewards)
			{
				if (!treeGui.questFile.isRewardClaimed(reward))
				{
					r++;
				}
			}

			if (r > 0 || quest.canRepeat)
			{
				list.add("");
			}

			if (r > 0)
			{
				list.add(I18n.format("ftbquests.gui.unclaimed_rewards") + ": " + TextFormatting.GOLD + r);
			}

			/*
			if (quest.canRepeat)
			{
				list.add(I18n.format("ftbquests.gui.times_completed") + ": " + TextFormatting.GOLD + treeGui.questFile.self.getTimesCompleted(quest));
			}
			*/
		}
	}

	@Override
	public WidgetType getWidgetType()
	{
		if (treeGui.selectedQuest == quest)
		{
			return WidgetType.MOUSE_OVER;
		}

		return treeGui.questFile.editingMode || quest.getVisibility(treeGui.questFile.self).isVisible() ? super.getWidgetType() : WidgetType.DISABLED;
	}

	@Override
	public void draw(Theme theme, int x, int y, int w, int h)
	{
		Color4I backgroundColor = Color4I.WHITE.withAlpha(100);
		Color4I outlineColor = Color4I.WHITE.withAlpha(150);
		Icon qicon = Icon.EMPTY;

		boolean cantStart = treeGui.questFile.self == null || !quest.canStartTasks(treeGui.questFile.self);

		if (!cantStart)
		{
			int progress = quest.getRelativeProgress(treeGui.questFile.self);

			if (progress >= 100)
			{
				boolean hasRewards = false;

				for (QuestReward reward : quest.rewards)
				{
					if (!treeGui.questFile.isRewardClaimed(reward))
					{
						hasRewards = true;
						break;
					}
				}

				if (hasRewards)
				{
					qicon = FTBQuestsTheme.ALERT;
				}
				else
				{
					qicon = FTBQuestsTheme.COMPLETED;
				}

				outlineColor = COL_COMPLETED.withAlpha(200);
			}
			else if (progress > 0)
			{
				outlineColor = COL_STARTED.withAlpha(200);
			}
		}
		else
		{
			outlineColor = Color4I.GRAY;
		}

		double s = treeGui.zoom * 3D / 2D;
		double sx = x + (w - s) / 2D;
		double sy = y + (h - s) / 2D;

		if (treeGui.selectedQuest == quest)
		{
			double s1 = s + treeGui.zoom / 5D;
			double sx1 = x + (w - s1) / 2D;
			double sy1 = y + (h - s1) / 2D;
			GlStateManager.pushMatrix();
			GlStateManager.translate(sx1, sy1, 0D);
			GlStateManager.scale(s1, s1, 1D);
			quest.shape.outline.draw(0, 0, 1, 1, Color4I.WHITE.withAlpha(150 + (int) (Math.sin(System.currentTimeMillis() * 0.003D) * 80)));
			GlStateManager.popMatrix();
		}

		GlStateManager.pushMatrix();
		GlStateManager.translate(sx, sy, 0D);
		GlStateManager.scale(s, s, 1D);
		quest.shape.background.draw(0, 0, 1, 1, backgroundColor);
		quest.shape.outline.draw(0, 0, 1, 1, outlineColor);
		GlStateManager.popMatrix();

		if (!icon.isEmpty())
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + (w - treeGui.zoom) / 2D, y + (h - treeGui.zoom) / 2D, 0D);
			icon.draw(0, 0, treeGui.zoom, treeGui.zoom);
			GlStateManager.popMatrix();
		}

		if (cantStart)
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(sx, sy, 500);
			GlStateManager.scale(s, s, 1D);
			quest.shape.shape.draw(0, 0, 1, 1, Color4I.BLACK.withAlpha(100));
			GlStateManager.popMatrix();
		}

		if (isMouseOver())
		{
			GlStateManager.pushMatrix();
			GlStateManager.translate(sx, sy, 500);
			GlStateManager.scale(s, s, 1D);
			quest.shape.shape.draw(0, 0, 1, 1, Color4I.WHITE.withAlpha(80));
			GlStateManager.popMatrix();
		}

		if (!qicon.isEmpty())
		{
			double s1 = treeGui.zoom / 2D;
			double os1 = s1 / 4D;
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + w - s1 - os1, y + os1, 500);
			GlStateManager.scale(s1, s1, 1D);
			qicon.draw(0, 0, 1, 1);
			GlStateManager.popMatrix();
		}
	}
}