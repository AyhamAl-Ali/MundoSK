package com.pie.tlatoani;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Slot;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.pie.tlatoani.CodeBlock.EffRunCodeBlock;
import com.pie.tlatoani.CodeBlock.ScopeSaveCodeBlock;
import com.pie.tlatoani.CodeBlock.SkriptCodeBlock;
import com.pie.tlatoani.CustomEvent.*;
import com.pie.tlatoani.Json.API.*;
import com.pie.tlatoani.Json.API.stream.JsonParser;
import com.pie.tlatoani.Json.API.stream.JsonParserFactory;
import com.pie.tlatoani.Json.EffPutJsonInListVariable;
import com.pie.tlatoani.Json.ExprListVariableAsJson;
import com.pie.tlatoani.Json.ExprStringAsJson;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.pie.tlatoani.Achievement.*;
import com.pie.tlatoani.Book.*;
import com.pie.tlatoani.EnchantedBook.*;
import com.pie.tlatoani.ListUtil.*;
import com.pie.tlatoani.Miscellaneous.*;
import com.pie.tlatoani.NoteBlock.*;
import com.pie.tlatoani.Probability.*;
import com.pie.tlatoani.ProtocolLib.*;
import com.pie.tlatoani.Socket.*;
import com.pie.tlatoani.TerrainControl.*;
import com.pie.tlatoani.Throwable.*;
import com.pie.tlatoani.Util.*;
import com.pie.tlatoani.WorldBorder.*;
import com.pie.tlatoani.WorldCreator.*;
import com.pie.tlatoani.WorldManagement.*;
import com.pie.tlatoani.Metrics.*;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.EnchantmentType;
import ch.njol.skript.util.Getter;
import ch.njol.skript.util.Timespan;

public class Mundo extends JavaPlugin{
	public static Mundo instance;
	public static FileConfiguration config;
    public static Boolean RandomSK;

    public static String pluginFolder;
    public static String separator;
    public static Map<String, JsonObject> worldLoaderSaver;
	
	public void onEnable(){
		instance = this;
		config = getConfig();
		config.addDefault("debug_mode", false);
		config.options().copyDefaults(true);
		saveConfig();
        RandomSK = Bukkit.getPluginManager().getPlugin("RandomSK") != null;
		Skript.registerAddon(this);
        pluginFolder = getDataFolder().getAbsolutePath();
        separator = File.separator;
        info("Pie is awesome :D");
        info("Loading worlds if there are any to load");
        loadWorlds(loadWorldLoader());
        //Achievement
        if (classInfoSafe(Achievement.class, "achievement")){
            Classes.registerClass(new ClassInfo<Achievement>(Achievement.class, "achievement").user(new String[]{"achievement"}).name("achievement").parser(new Parser<Achievement>(){

                public Achievement parse(String s, ParseContext context) {
                	try {
                		return Achievement.valueOf(s.toUpperCase());
                	} catch (IllegalArgumentException e) {
                		return null;
                	}
                }

                public String toString(Achievement ach, int flags) {
        		return ach.toString();
            }

                public String toVariableNameString(Achievement ach) {
        		return ach.toString();
            }

                public String getVariableNamePattern() {
                return ".+";
            }
            }));
        }
		Skript.registerEffect(EffAwardAch.class, "award achieve[ment] %achievement% to %player%");
		Skript.registerEffect(EffRemoveAch.class, "remove achieve[ment] %achievement% from %player%");
		Skript.registerEvent("Achievement Award", EvtAchAward.class, PlayerAchievementAwardedEvent.class, "achieve[ment] [%-achievement%] award", "award of achieve[ment] [%-achievement%]");
		EventValues.registerEventValue(PlayerAchievementAwardedEvent.class, Player.class, new Getter<Player, PlayerAchievementAwardedEvent>() {
			public Player get(PlayerAchievementAwardedEvent e) {
				return e.getPlayer();
			}
		}, 0);
		EventValues.registerEventValue(PlayerAchievementAwardedEvent.class, Achievement.class, new Getter<Achievement, PlayerAchievementAwardedEvent>() {
			public Achievement get(PlayerAchievementAwardedEvent e) {
				return e.getAchievement();
			}
		}, 0);
		Skript.registerExpression(ExprParentAch.class,Achievement.class,ExpressionType.PROPERTY,"parent of achieve[ment] %achievement%");
		Skript.registerExpression(ExprAllAch.class,Achievement.class,ExpressionType.PROPERTY,"[all] achieve[ment]s [of %-player%]", "%player%'s achieve[ment]s");
		Skript.registerExpression(ExprHasAch.class,Boolean.class,ExpressionType.PROPERTY,"%player% has achieve[ment] %achievement%");
		//Book
        ListUtil.registerTransformer(TransBookPages.class, "page");
		Skript.registerExpression(ExprBook.class,ItemStack.class,ExpressionType.COMBINED,"%itemstack% titled %string%, [written] by %string%, [with] pages %strings%");
		Skript.registerExpression(ExprTitleOfBook.class,String.class,ExpressionType.PROPERTY,"title of %itemstack%");
		Skript.registerExpression(ExprAuthorOfBook.class,String.class,ExpressionType.PROPERTY,"author of %itemstack%");
		//CodeBlock
        Classes.registerClass(new ClassInfo<SkriptCodeBlock>(SkriptCodeBlock.class, "codeblock").user(new String[]{"codeblock"}).name("codeblock").parser(new Parser<SkriptCodeBlock>(){

            public SkriptCodeBlock parse(String s, ParseContext context) {
                return null;
            }

            public String toString(SkriptCodeBlock codeBlock, int flags) {
                return null;
            }

            public String toVariableNameString(SkriptCodeBlock codeBlock) {
                return null;
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
        Skript.registerCondition(ScopeSaveCodeBlock.class, "codeblock %object%");
        Skript.registerEffect(EffRunCodeBlock.class, "run codeblock %codeblock% [(1¦here|2¦with %-objects%)]");
        //CustomEvent
        Skript.registerEffect(EffCallCustomEvent.class, "call custom event %string% [to] [det[ail]s %-objects%] [arg[ument]s %-objects%]");
        Skript.registerEvent("Custom Event", EvtCustomEvent.class, UtilCustomEvent.class, "evt %strings%");
        Skript.registerExpression(ExprIDOfCustomEvent.class,String.class,ExpressionType.PROPERTY,"id of custom event", "custom event's id");
        Skript.registerExpression(ExprArgsOfCustomEvent.class,Object.class,ExpressionType.PROPERTY,"args of custom event", "custom event's args");
        //EnchantedBook
		Skript.registerExpression(ExprEnchBookWithEnch.class,ItemStack.class,ExpressionType.PROPERTY,"%itemstack% containing %enchantmenttypes%");
		Skript.registerExpression(ExprEnchantLevelInEnchBook.class,Integer.class,ExpressionType.PROPERTY,"level of %enchantmenttype% within %itemstack%");
		Skript.registerExpression(ExprEnchantsInEnchBook.class,EnchantmentType.class,ExpressionType.PROPERTY,"enchants within %itemstack%");
		//Json
        Classes.registerClass(new ClassInfo<JsonObject>(JsonObject.class, "jsonobject").user(new String[]{"jsonobject"}).name("jsonobject").parser(new Parser<JsonObject>(){

            public JsonObject parse(String s, ParseContext context) {
                return null;
            }

            public String toString(JsonObject jsonObject, int flags) {
                return jsonObject.toString();
            }

            public String toVariableNameString(JsonObject jsonObject) {
                return jsonObject.toString();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
        Skript.registerEffect(EffPutJsonInListVariable.class, "put json %jsonobject% in listvar %objects%", "put jsons %jsonobjects% in listvar %objects%");
        Skript.registerExpression(ExprListVariableAsJson.class, JsonObject.class, ExpressionType.PROPERTY, "json of listvar %objects%", "jsons of listvar %objects%");
        Skript.registerExpression(ExprStringAsJson.class, JsonObject.class, ExpressionType.PROPERTY, "json of string %string%");
        //ListUtil
        //Skript.registerEffect(EffInsertItem.class, "(add|insert) %objects% (1¦before|0¦after) (<[a-zA-z]+> %-number%|last <[a-zA-z]+>)[ (of|in) %-object/objects%]");
        Skript.registerEffect(EffMoveItem.class, "move %objects% (-1¦front|-1¦forward[s]|1¦back[ward[s]]) %number%");
        /*
        Skript.registerExpression(ExprItem.class,Object.class,ExpressionType.PROPERTY,"(<[a-zA-z]+> %-number%|last <[a-zA-z]+>)[ of %-object/objects%]");
        Skript.registerExpression(ExprItems.class,Object.class,ExpressionType.PROPERTY,"<[a-zA-z]+>[ of %-object/objects%]");
        Skript.registerExpression(ExprSomeItems.class,String.class,ExpressionType.PROPERTY,"<[a-zA-z]+> %-number% to (%-number%|last)[ of %-object/objects%]");
        Skript.registerExpression(ExprItemCount.class,Number.class,ExpressionType.PROPERTY,"<[a-zA-z]+> count[ of %-objects%]");
        */
        ListUtil.registerTransformer(TransDefault.class, "item");
        //Miscellaneous
		Classes.registerClass(new ClassInfo<Difficulty>(Difficulty.class, "difficulty").user(new String[]{"difficulty"}).name("difficulty").parser(new Parser<Difficulty>(){

            public Difficulty parse(String s, ParseContext context) {
            	try {
            		return Difficulty.valueOf(s.toUpperCase());
            	} catch (IllegalArgumentException e) {
            		return null;
            	}
            }

            public String toString(Difficulty diff, int flags) {
        		return diff.toString().toLowerCase();
            }

            public String toVariableNameString(Difficulty diff) {
        		return diff.toString().toLowerCase();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
		Skript.registerEvent("Hang Event", EvtHang.class, HangingPlaceEvent.class, "hang");
		EventValues.registerEventValue(HangingBreakEvent.class, Entity.class, new Getter<Entity, HangingBreakEvent>() {
			@Override
			public Entity get(HangingBreakEvent hangingEvent) {
				return hangingEvent instanceof HangingBreakByEntityEvent ? ((HangingBreakByEntityEvent) hangingEvent).getRemover() : null;
			}
		}, 0);
		EventValues.registerEventValue(HangingPlaceEvent.class, Block.class, new Getter<Block, HangingPlaceEvent>() {
			@Override
			public Block get(HangingPlaceEvent hangingPlaceEvent) {
				return hangingPlaceEvent.getBlock();
			}
		}, 0);
		Skript.registerEvent("Unhang Event", EvtUnHang.class, HangingBreakEvent.class, "unhang");;
        Skript.registerEvent("Chat Tab Complete Event", SimpleEvent.class, PlayerChatTabCompleteEvent.class, "chat tab complete");
        EventValues.registerEventValue(PlayerChatTabCompleteEvent.class, String.class, new Getter<String, PlayerChatTabCompleteEvent>() {
            @Override
            public String get(PlayerChatTabCompleteEvent playerChatTabCompleteEvent) {
                return playerChatTabCompleteEvent.getChatMessage();
            }
        }, 0);
        Skript.registerEvent("Armor Stand Interact Event", SimpleEvent.class, PlayerArmorStandManipulateEvent.class, "armor stand (manipulate|interact)");
        EventValues.registerEventValue(PlayerArmorStandManipulateEvent.class, Entity.class, new Getter<Entity, PlayerArmorStandManipulateEvent>() {
            @Override
            public Entity get(PlayerArmorStandManipulateEvent playerArmorStandManipulateEvent) {
                return playerArmorStandManipulateEvent.getRightClicked();
            }
        }, 0);
        EventValues.registerEventValue(PlayerArmorStandManipulateEvent.class, ItemStack.class, new Getter<ItemStack, PlayerArmorStandManipulateEvent>() {
            @Override
            public ItemStack get(PlayerArmorStandManipulateEvent playerArmorStandManipulateEvent) {
                return playerArmorStandManipulateEvent.getArmorStandItem();
            }
        }, 0);
        EventValues.registerEventValue(PlayerArmorStandManipulateEvent.class, Slot.class, new Getter<Slot, PlayerArmorStandManipulateEvent>() {
            @Override
            public Slot get(PlayerArmorStandManipulateEvent playerArmorStandManipulateEvent) {
                return new ArmorStandEquipmentSlot(playerArmorStandManipulateEvent.getRightClicked(), ArmorStandEquipmentSlot.EquipSlot.getByEquipmentSlot(playerArmorStandManipulateEvent.getSlot()));
            }
        }, 0);
        Skript.registerEvent("Armor Stand Place Event", EvtArmorStandPlace.class, EntitySpawnEvent.class, "armor stand place");
        Skript.registerExpression(ExprLastToken.class, String.class, ExpressionType.SIMPLE, "last token");
        Skript.registerExpression(ExprHangedEntity.class,Entity.class,ExpressionType.SIMPLE,"hanged entity");
		Skript.registerExpression(ExprWorldString.class,World.class,ExpressionType.PROPERTY,"world %string%");
		Skript.registerExpression(ExprHighestSolidBlock.class,Block.class,ExpressionType.PROPERTY,"highest [(solid|non-air)] block at %location%");
		Skript.registerExpression(ExprDifficulty.class,Difficulty.class,ExpressionType.PROPERTY,"difficulty of %world%");
		Skript.registerExpression(ExprGameRule.class,String.class,ExpressionType.PROPERTY,"value of [game]rule %string% in %world%");
		Skript.registerExpression(ExprReturnTypeOfFunction.class,ClassInfo.class,ExpressionType.PROPERTY,"return type of function %string%");
        Skript.registerExpression(ExprRemainingAir.class,Timespan.class,ExpressionType.PROPERTY,"breath of %livingentity%", "%livingentity%'s breath", "max breath of %livingentity%", "%livingentity%'s max breath");
		Skript.registerExpression(ExprLoadedScripts.class,String.class,ExpressionType.SIMPLE, "loaded scripts");
        ListUtil.registerTransformer(TransCompletions.class, "completion");
        //NoteBlock
        Classes.registerClass(new ClassInfo<Note>(Note.class, "note").user(new String[]{"note"}).name("note").parser(new Parser<Note>(){

            public Note parse(String s, ParseContext context) {
                if (s.substring(0, 1).toUpperCase().equals("N")) {
                    s = s.substring(1);
                } else {
                    if (RandomSK) {
                        return null;
                    }
                }
                if (s.length() > 3) {
                    return null;
                }
                try {
                    Note.Tone tone = Note.Tone.valueOf(s.substring(0, 1).toUpperCase());
                    s = s.substring(1);
                    Boolean sharp = null;
                    Integer octave = 0;
                    if (s.length() > 0) {
                        if (s.substring(0, 1).equals("+")) {
                            sharp = true;
                            s = s.substring(1);
                        } else if (s.substring(0, 1).equals("-")) {
                            sharp = false;
                            s = s.substring(1);
                        } else if (s.length() > 1) {
                            return null;
                        }
                    }
                    if (s.length() > 0) {
                        if (s.equals("1")) {
                            octave = 1;
                        } else if (s.equals("2")) {
                            octave = 2;
                        } else if (s.equals("0")) {
                            octave = 0;
                        } else {
                            return null;
                        }
                    }
                    if (sharp == null) {
                        return Note.natural(octave, tone);
                    } else if (sharp) {
                        return Note.sharp(octave, tone);
                    } else {
                        return Note.flat(octave, tone);
                    }
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }

            public String toString(Note note, int flags) {
                String result = note.getTone().toString();
                if (note.isSharped()) {
                    result += '+';
                }
                if (note.getOctave() > 0) {
                    result = result + Integer.toString(note.getOctave());
                }
                return result;
            }

            public String toVariableNameString(Note note) {
                String result = note.getTone().toString();
                if (note.isSharped()) {
                    result += '+';
                }
                if (note.getOctave() > 0) {
                    result = result + Integer.toString(note.getOctave());
                }
                return result;
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
        Classes.registerClass(new ClassInfo<Instrument>(Instrument.class, "instrument").user(new String[]{"instrument"}).name("instrument").parser(new Parser<Instrument>(){

            public Instrument parse(String s, ParseContext context) {
                try {
                    return Instrument.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }

            public String toString(Instrument instrument, int flags) {
                return instrument.toString().toLowerCase();
            }

            public String toVariableNameString(Instrument instrument) {
                return instrument.toString().toLowerCase();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
        Skript.registerEffect(EffPlayNoteBlock.class, "play [[%-note% with] %-instrument% on] noteblock %block%");
        Skript.registerEvent("Note Play", SimpleEvent.class, NotePlayEvent.class, "note play");
        EventValues.registerEventValue(NotePlayEvent.class, Note.class, new Getter<Note, NotePlayEvent>(){

            @Override
            public Note get(NotePlayEvent notePlayEvent) {
                return notePlayEvent.getNote();
            }
        }, 0);
        EventValues.registerEventValue(NotePlayEvent.class, Instrument.class, new Getter<Instrument, NotePlayEvent>(){

            @Override
            public Instrument get(NotePlayEvent notePlayEvent) {
                return notePlayEvent.getInstrument();
            }
        }, 0);
        EventValues.registerEventValue(NotePlayEvent.class, Block.class, new Getter<Block, NotePlayEvent>(){

            @Override
            public Block get(NotePlayEvent notePlayEvent) {
                return notePlayEvent.getBlock();
            }
        }, 0);
        Skript.registerExpression(ExprNoteOfBlock.class, Note.class, ExpressionType.PROPERTY, "note of %block%", "%block%'s note");
        //Probability
		Skript.registerCondition(ScopeProbability.class, "prob[ability]", "random chance");
		Skript.registerCondition(CondProbability.class, "%number%[1¦\\%] prob[ability]");
		Skript.registerExpression(ExprRandomIndex.class,String.class,ExpressionType.PROPERTY,"random from %numbers% prob[abilitie]s");
		Skript.registerExpression(ExprRandomNumberIndex.class,Integer.class,ExpressionType.PROPERTY,"random number from %numbers% prob[abilitie]s");
		//ProtocolLib
		if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            Mundo.info("You've discovered the amazing realm of ProtocolLib packet syntaxes!");
			Classes.registerClass(new ClassInfo<PacketType>(PacketType.class, "packettype").user(new String[]{"packettype"}).name("packettype").parser(new Parser<PacketType>(){

				public PacketType parse(String s, ParseContext context) {
					return ExprAllPacketTypes.fromString(s.toLowerCase());
				}

				public String toString(PacketType packetType, int flags) {
					return ExprAllPacketTypes.PacketTypeToString(packetType);
				}

				public String toVariableNameString(PacketType packetType) {
					return ExprAllPacketTypes.PacketTypeToString(packetType);
				}

				public String getVariableNamePattern() {
					return ".+";
				}
			}));
			Classes.registerClass(new ClassInfo<PacketContainer>(PacketContainer.class, "packet").user(new String[]{"packet"}).name("packet").parser(new Parser<PacketContainer>(){

				public PacketContainer parse(String s, ParseContext context) {
					return null;
				}

				public String toString(PacketContainer packet, int flags) {
					return null;
				}

				public String toVariableNameString(PacketContainer packet) {
					return null;
				}

				public String getVariableNamePattern() {
					return ".+";
				}
			}));
			Skript.registerEffect(EffSendPacket.class, "send packet %packet% to %player%", "send %player% packet %packet%");
			Skript.registerEvent("Packet Event", EvtPacketEvent.class, UtilPacketEvent.class, "packet event %packettypes%");
			EventValues.registerEventValue(UtilPacketEvent.class, PacketContainer.class, new Getter<PacketContainer, UtilPacketEvent>() {
				@Override
				public PacketContainer get(UtilPacketEvent e) {
					return e.getPacket();
				}
			}, 0);
			EventValues.registerEventValue(UtilPacketEvent.class, PacketType.class, new Getter<PacketType, UtilPacketEvent>() {
				@Override
				public PacketType get(UtilPacketEvent e) {
					return e.getPacketType();
				}
			}, 0);
			EventValues.registerEventValue(UtilPacketEvent.class, Player.class, new Getter<Player, UtilPacketEvent>() {
				@Override
				public Player get(UtilPacketEvent e) {
					return e.getPlayer();
				}
			}, 0);
			Skript.registerExpression(ExprAllPacketTypes.class, PacketType.class, ExpressionType.SIMPLE, "all packettypes");
			Skript.registerExpression(ExprNewPacket.class, PacketContainer.class, ExpressionType.PROPERTY, "new %packettype% packet");
            Skript.registerExpression(ExprJsonObjectOfPacket.class, JsonObject.class, ExpressionType.PROPERTY, "%string% pjson %number% of %packet%");
            Skript.registerExpression(ExprJsonObjectArrayOfPacket.class, JsonObject.class, ExpressionType.PROPERTY, "%string% array pjson %number% of %packet%");
            Skript.registerExpression(ExprObjectOfPacket.class, Object.class, ExpressionType.PROPERTY, "%*classinfo% pinfo %number% of %packet%", "%*classinfo% array pinfo %number% of %packet%","%string% pinfo %number% of %packet%");
            Skript.registerExpression(ExprPrimitiveOfPacket.class, Number.class, ExpressionType.PROPERTY, "(0¦byte|1¦short|2¦int|3¦long|4¦float|5¦double) pnum %number% of %packet%");
            Skript.registerExpression(ExprPrimitiveArrayOfPacket.class, Number.class, ExpressionType.PROPERTY, "(0¦int|1¦byte) array pnum %number% of %packet%");
		}
		//Socket
		Skript.registerEffect(EffWriteToSocket.class, "write %strings% to socket with host %string% port %number% [with timeout %-timespan%] [to handle response through function %-string% with id %-string%]");
		Skript.registerEffect(EffOpenFunctionSocket.class, "open function socket at port %number% [with password %-string%] [through function %-string%]");
		Skript.registerEffect(EffCloseFunctionSocket.class, "close function socket at port %number%");
		Skript.registerExpression(ExprPassOfFunctionSocket.class,String.class,ExpressionType.PROPERTY,"pass[word] of function socket at port %number%");
		Skript.registerExpression(ExprHandlerOfFunctionSocket.class,String.class,ExpressionType.PROPERTY,"handler [function] of function socket at port %number%");
		Skript.registerExpression(ExprFunctionSocketIsOpen.class,Boolean.class,ExpressionType.PROPERTY,"function socket is open at port %number%");
		Skript.registerExpression(ExprServerSocketIsOpen.class,Boolean.class,ExpressionType.COMBINED,"server socket is open at host %string% port %number% [with timeout of %-timespan%]");
		Skript.registerExpression(ExprMotdOfServer.class,String.class,ExpressionType.COMBINED,"motd of server with host %string% [port %-number%]");
		Skript.registerExpression(ExprPlayerCountOfServer.class,Number.class,ExpressionType.COMBINED,"(1¦player count|0¦max player count) of server with host %string% [port %-number%]");
		//TerrainControl
		if (Bukkit.getServer().getPluginManager().getPlugin("TerrainControl") != null) {
			this.getLogger().info("You uncovered the secret TerrainControl syntaxes!");
			Skript.registerEffect(EffSpawnObject.class, "(tc|terrain control) spawn %string% at %location% with rotation %string%");
			Skript.registerExpression(ExprBiomeAt.class,String.class,ExpressionType.PROPERTY,"(tc|terrain control) biome at %location%");
			Skript.registerExpression(ExprTCEnabled.class,Boolean.class,ExpressionType.PROPERTY,"(tc|terrain control) is enabled for %world%");
		}
		//Throwable
		Classes.registerClass(new ClassInfo<Throwable>(Throwable.class, "throwable").user(new String[]{"throwable"}).name("throwable").parser(new Parser<Throwable>(){

            public Throwable parse(String s, ParseContext context) {
                return null;
            }

            public String toString(Throwable exc, int flags) {
                return exc.toString();
            }

            public String toVariableNameString(Throwable exc) {
                return exc.toString();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
		Classes.registerClass(new ClassInfo<StackTraceElement>(StackTraceElement.class, "stacktraceelement").user(new String[]{"stacktraceelement"}).name("stacktraceelement").parser(new Parser<StackTraceElement>(){

            public StackTraceElement parse(String s, ParseContext context) {
                return null;
            }

            public String toString(StackTraceElement elem, int flags) {
                return elem.toString();
            }

            public String toVariableNameString(StackTraceElement elem) {
                return elem.toString();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
		Skript.registerCondition(ScopeTry.class, "try");
		Skript.registerCondition(CondCatch.class, "catch in %object%");
		Skript.registerEffect(EffPrintStackTrace.class, "print stack trace of %throwable%");
		if (Bukkit.getServer().getPluginManager().getPlugin("RandomSK") == null)
		Skript.registerExpression(ExprCatch.class,Throwable.class,ExpressionType.SIMPLE,"(catch|caught exception)");
		else
		Skript.registerExpression(ExprCatch.class,Throwable.class,ExpressionType.SIMPLE,"caught exception");
		Skript.registerExpression(ExprCause.class,Throwable.class,ExpressionType.PROPERTY,"throwable cause of %throwable%", "%throwable%'s throwable cause");
		Skript.registerExpression(ExprDetails.class,String.class,ExpressionType.PROPERTY,"details of %throwable%", "%throwable%'s details");
		Skript.registerExpression(ExprStackTrace.class,StackTraceElement.class,ExpressionType.PROPERTY,"stack trace of %throwable%", "%throwable%'s stack trace");
		Skript.registerExpression(ExprPropertyNameOfSTE.class,String.class,ExpressionType.PROPERTY,"(0¦class|1¦file|2¦method) name of %stacktraceelement%", "%stacktraceelement%'s (0¦class|1¦file|2¦method) name");
		Skript.registerExpression(ExprLineNumberOfSTE.class,Integer.class,ExpressionType.PROPERTY,"line number of %stacktraceelement%", "%stacktraceelement%'s line number");
		//Util
        Skript.registerEffect(EffScope.class, "$ scope");
        Skript.registerExpression(ExprLoopWhile.class,Object.class,ExpressionType.PROPERTY,"%objects% while %boolean%");
        Skript.registerExpression(ExprTreeOfListVariable.class, Object.class, ExpressionType.PROPERTY, "tree of %objects%");
        Skript.registerExpression(ExprBranch.class, String.class, ExpressionType.PROPERTY, "branch");
		//WorldBorder
		Skript.registerEffect(EffResetBorder.class, "reset %world%");
		Skript.registerEvent("Border Stabilize", EvtBorderStabilize.class, UtilBorderStabilizeEvent.class, "border stabilize [in %-world%]");
		EventValues.registerEventValue(UtilBorderStabilizeEvent.class, World.class, new Getter<World, UtilBorderStabilizeEvent>() {
			@Override
			public World get(UtilBorderStabilizeEvent e) {
				return e.getWorld();
			}
		}, 0);
		Skript.registerExpression(ExprSizeOfBorder.class,Double.class,ExpressionType.PROPERTY,"size of %world% [over %-timespan%]");
		Skript.registerExpression(ExprCenterOfBorder.class,Location.class,ExpressionType.PROPERTY,"center of %world%");
		Skript.registerExpression(ExprDamageAmountOfBorder.class,Double.class,ExpressionType.PROPERTY,"damage amount of %world%");
		Skript.registerExpression(ExprDamageBufferOfBorder.class,Double.class,ExpressionType.PROPERTY,"damage buffer of %world%");
		Skript.registerExpression(ExprWarningDistanceOfBorder.class,Integer.class,ExpressionType.PROPERTY,"warning distance of %world%");
		Skript.registerExpression(ExprWarningTimeOfBorder.class,Integer.class,ExpressionType.PROPERTY,"warning time of %world%");
		Skript.registerExpression(ExprFinalSizeOfBorder.class,Double.class,ExpressionType.PROPERTY,"final size of %world%");
		Skript.registerExpression(ExprTimeRemainingUntilBorderStabilize.class,Timespan.class,ExpressionType.PROPERTY,"time remaining until border stabilize in %world%");
		Skript.registerExpression(ExprBeyondBorder.class,Boolean.class,ExpressionType.PROPERTY,"%location% is (1¦within|0¦beyond) border");
		//WorldCreator
		Classes.registerClass(new ClassInfo<WorldCreator>(WorldCreator.class, "creator").user(new String[]{"creator"}).name("creator").parser(new Parser<WorldCreator>(){

            public WorldCreator parse(String s, ParseContext context) {
                return null;
            }

            public String toString(WorldCreator creator, int flags) {
                return creator.toString();
            }

            public String toVariableNameString(WorldCreator creator) {
                return creator.toString();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
		if (Bukkit.getServer().getPluginManager().getPlugin("RandomSK") == null) {
			Classes.registerClass(new ClassInfo<Environment>(Environment.class, "environment").user(new String[]{"environment"}).name("environment").parser(new Parser<Environment>(){

	            public Environment parse(String s, ParseContext context) {
	            	if (s.equalsIgnoreCase("NORMAL")) return (World.Environment.NORMAL);
	    			if (s.equalsIgnoreCase("NETHER")) return (World.Environment.NETHER);
	    			if (s.equalsIgnoreCase("END") || s.equalsIgnoreCase("THE_END")) return (World.Environment.THE_END);
	                return null;
	            }

	            public String toString(Environment env, int flags) {
	        		if (env == World.Environment.NORMAL) return "normal";
	        		if (env == World.Environment.NETHER) return "nether";
	        		if (env == World.Environment.THE_END) return "end";
	        		return null;
	            }

	            public String toVariableNameString(Environment env) {
	            	if (env == World.Environment.NORMAL) return "normal";
	        		if (env == World.Environment.NETHER) return "nether";
	        		if (env == World.Environment.THE_END) return "end";
	        		return null;
	            }

	            public String getVariableNamePattern() {
	                return ".+";
	            }
	        }));
		}
		Classes.registerClass(new ClassInfo<WorldType>(WorldType.class, "worldtype").user(new String[]{"worldtype"}).name("worldtype").parser(new Parser<WorldType>(){

            public WorldType parse(String s, ParseContext context) {
            	if (s.equalsIgnoreCase("normal")) return (WorldType.NORMAL);
    			if (s.equalsIgnoreCase("flat") || s.equalsIgnoreCase("superflat")) return (WorldType.FLAT);
    			if (s.equalsIgnoreCase("large biomes") || s.equalsIgnoreCase("large_biomes")) return (WorldType.LARGE_BIOMES);
    			if (s.equalsIgnoreCase("amplified")) return (WorldType.AMPLIFIED);
    			if (s.equalsIgnoreCase("version 1.1") || s.equalsIgnoreCase("version_1_1")) return (WorldType.VERSION_1_1);
    			if (s.equalsIgnoreCase("customized")) return (WorldType.CUSTOMIZED);
                return null;
            }

            public String toString(WorldType type, int flags) {
            	if (type == WorldType.NORMAL) return "normal";
        		if (type == WorldType.AMPLIFIED) return "amplified";
        		if (type == WorldType.FLAT) return "flat";
        		if (type == WorldType.LARGE_BIOMES) return "large biomes";
        		if (type == WorldType.VERSION_1_1) return "version 1.1";
        		if (type == WorldType.CUSTOMIZED) return "customized";
        		return null;
            }

            public String toVariableNameString(WorldType type) {
            	if (type == WorldType.NORMAL) return "normal";
        		if (type == WorldType.AMPLIFIED) return "amplified";
        		if (type == WorldType.FLAT) return "flat";
        		if (type == WorldType.LARGE_BIOMES) return "large biomes";
        		if (type == WorldType.VERSION_1_1) return "version 1.1";
        		if (type == WorldType.CUSTOMIZED) return "customized";
        		return null;
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
		Skript.registerExpression(ExprCreatorNamed.class,WorldCreator.class,ExpressionType.PROPERTY,"creator (with name|named) %string%");
		Skript.registerExpression(ExprCreatorWith.class,WorldCreator.class,ExpressionType.PROPERTY,"%creator%[ modified],[ name %-string%][,][ env[ironment] %-environment%][,][ seed %-string%][,][ type %-worldtype%][,][ gen[erator] %-string%][,][ gen[erator] settings %-string%][,][ struct[ures] %-boolean%]");
		Skript.registerExpression(ExprCreatorOf.class,WorldCreator.class,ExpressionType.PROPERTY,"creator of %world%");
		Skript.registerExpression(ExprNameOfCreator.class,String.class,ExpressionType.PROPERTY,"worldname of %creator%");
		Skript.registerExpression(ExprEnvOfCreator.class,Environment.class,ExpressionType.PROPERTY,"env[ironment] of %creator%");
		Skript.registerExpression(ExprSeedOfCreator.class,String.class,ExpressionType.PROPERTY,"seed of %creator%");
		Skript.registerExpression(ExprGenOfCreator.class,String.class,ExpressionType.PROPERTY,"gen[erator] of %creator%");
		Skript.registerExpression(ExprGenSettingsOfCreator.class,String.class,ExpressionType.PROPERTY,"gen[erator] setSafely[tings] of %creator%");
		Skript.registerExpression(ExprTypeOfCreator.class,WorldType.class,ExpressionType.PROPERTY,"worldtype of %creator%");
		Skript.registerExpression(ExprStructOfCreator.class,Boolean.class,ExpressionType.PROPERTY,"struct[ure(s| settings)] of %creator%");
		//WorldManagement
        Skript.registerEffect(EffCreateWorld.class, "create world named %string%[,][ env[ironment] %-environment%][,][ seed %-string%][,][ type %-worldtype%][,][ gen[erator] %-string%][,][ gen[erator] settings %-string%][,][ struct[ures] %-boolean%]");
		Skript.registerEffect(EffCreateWorldCreator.class, "create world using %creator%");
		Skript.registerEffect(EffUnloadWorld.class, "unload %world% [save %-boolean%]");
		Skript.registerEffect(EffDeleteWorld.class, "delete %world%");
		Skript.registerEffect(EffDuplicateWorld.class, "duplicate %world% using name %string%");
        Skript.registerEffect(EffRunCreatorOnStart.class, "run %creator% on start");
        Skript.registerEffect(EffDoNotLoadWorldOnStart.class, "don't load world %string% on start");
        Skript.registerExpression(ExprCurrentWorlds.class,World.class,ExpressionType.SIMPLE,"[all] current worlds");
		//TestSyntaxes
		//
		try {
			Field classinfos = Classes.class.getDeclaredField("tempClassInfos");
			classinfos.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<ClassInfo<?>> classes = (List<ClassInfo<?>>) classinfos.get(null);
			for (int i = 0; i < classes.size(); i++)
				registerCustomEventValue(classes.get(i));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        ListUtil.register();
		info("Awesome syntaxes have been registered!");
		try {
	        Metrics metrics = new Metrics(this);
	        Graph skriptv = metrics.createGraph("Skript Version");
	        skriptv.addPlotter(new Metrics.Plotter(Bukkit.getServer().getPluginManager().getPlugin("Skript").getDescription().getVersion()){
	        	@Override
	        	public int getValue() {
	        		return 1;
	        	}
	        });
	        Graph addons = metrics.createGraph("Skript Addons");
	        Object[] addonlist = Skript.getAddons().toArray();
	        for (int i = 0; i < addonlist.length; i++) {
	        	addons.addPlotter(new Metrics.Plotter(((SkriptAddon) addonlist[i]).getName()) {
					
					@Override
					public int getValue() {
						return 1;
					}
				});
	        }
	        metrics.start();
	        info("Metrics have been enabled!");
	    } catch (Exception e) {
	    	info("Metrics failed to enable");
	        Mundo.reportException(this, e);
	    }
	}

    @Override
    public void onDisable() {
        info("Closing all function sockets");
        UtilFunctionSocket.onDisable();
        info("Saving world loaders");
        saveWorldLoader();
    }
	
	public static void reportException(Object o, Exception e) {
		info("An exception has occured within MundoSK");
		info("Please report this to the MundoSK thread on forums.skunity.com");
		info("Exception at " + o.getClass().getSimpleName());
		e.printStackTrace();
	}
	
	public static void info(String s) {
		instance.getLogger().info(s);
	}
	
	public static void debug(Object obj, String msg) {
		if (config.getBoolean("debug_mode")) {
			info(obj.getClass().getSimpleName() + ": " + msg);
		}
	}
	
	public static void classDebug(Class<?> cla, String msg) {
		if (config.getBoolean("debug_mode")) {
			info(cla.getSimpleName() + ": " + msg);
		}
	}

	public static void debug(Object obj, Exception e) {
		if (config.getBoolean("debug_mode")) {
			reportException(obj, e);
		}
	}
	
	public static <T> void registerCustomEventValue(ClassInfo<T> type) {
		EventValues.registerEventValue(UtilCustomEvent.class, type.getC(), new Getter<T, UtilCustomEvent>() {
			@SuppressWarnings("unchecked")
			@Override
			public T get(UtilCustomEvent e) {
				return (T) e.getDetail(type);
			}
		}, 0);
	}

    public static Boolean classInfoSafe(Class c, String name) {
        if (Classes.getExactClassInfo(c) != null) {
            return false;
        }
        if (Classes.getClassInfoNoError(name) != null) {
            return false;
        }
        return true;
    }

    public static JsonObject loadWorldLoader() {
        try {
            File loaderFile = new File(pluginFolder + separator + "worldloader.json");
            if (!loaderFile.exists()) {
                loaderFile.createNewFile();
                JsonObject emptyObject = Json.createObjectBuilder().build();
                FileWriter writer = new FileWriter(loaderFile);
                writer.write(emptyObject.toString());
                writer.flush();
                writer.close();
            }
            JsonReader reader = Json.createReader(new FileReader(loaderFile));
            JsonObject worldLoaders = reader.readObject();
            final Map<String, JsonObject> saver = new HashMap<String, JsonObject>();
            worldLoaders.forEach(new BiConsumer<String, JsonValue>() {
                @Override
                public void accept(String s, JsonValue jsonValue) {
                    saver.put(s, (JsonObject) jsonValue);
                }
            });
            worldLoaderSaver = saver;
            return worldLoaders;
        } catch (IOException e) {
            info("A problem occurred while loading worlds");
            debug(Mundo.class, e);
        }
        worldLoaderSaver = new HashMap<String, JsonObject>();
        return Json.createObjectBuilder().build();
    }

    public static void loadWorlds(JsonObject worldLoaders) {
        worldLoaders.forEach(new BiConsumer<String, JsonValue>() {
            @Override
            public void accept(String s, JsonValue jsonValue) {
                WorldCreator creator = new WorldCreator(s);
                JsonObject creatorJson = (JsonObject) jsonValue;
                creator.environment(World.Environment.valueOf(creatorJson.getString("environment")));
                creator.type(WorldType.valueOf(creatorJson.getString("worldtype")));
                creator.generateStructures(creatorJson.getBoolean("structures"));
                creator.seed(Long.parseLong(creatorJson.getString("seed")));
                /*String generator;
                if ((generator = creatorJson.getString("generator", null)) != null) {
                    creator.generator(generator);
                }*/
                String generatorSettings;
                if ((generatorSettings = creatorJson.getString("generatorsettings", null)) != null) {
                    creator.generatorSettings(generatorSettings);
                }
                info("Loading the world '" + s + "'");
                creator.createWorld();
                info("The world '" + s + "' has been loaded!");
            }
        });
    }

    public static void addWorldLoader(WorldCreator creator) {
        JsonObjectBuilder creatorJsonBuilder = Json.createObjectBuilder();
        creatorJsonBuilder.add("environment", creator.environment().toString());
        creatorJsonBuilder.add("worldtype", creator.type().toString());
        creatorJsonBuilder.add("structures", creator.generateStructures());
        creatorJsonBuilder.add("seed", new Long(creator.seed()).toString());
        if (creator.generatorSettings() != null) {
            creatorJsonBuilder.add("generatorsettings", creator.generatorSettings());
        }
        JsonObject creatorJson = creatorJsonBuilder.build();
        worldLoaderSaver.put(creator.name(), creatorJson);
    }

    public static void removeWorldLoader(String worldname) {
        worldLoaderSaver.remove(worldname);
    }

    public static void saveWorldLoader() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        worldLoaderSaver.forEach(new BiConsumer<String, JsonObject>() {
            @Override
            public void accept(String s, JsonObject jsonObject) {
                builder.add(s, jsonObject);
            }
        });
        try {
            File loaderFile = new File(pluginFolder + separator + "worldloader.json");
            FileWriter writer = new FileWriter(loaderFile);
            writer.write(builder.build().toString());
            writer.flush();
            writer.close();

        } catch (IOException e) {
            info("A problem occured while saving world loaders");
            debug(Mundo.class, e);
        }

    }
	
}
