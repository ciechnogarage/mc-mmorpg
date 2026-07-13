package pl.mcmmorpg.characterstage;

import org.bukkit.plugin.java.JavaPlugin;

public final class CharacterStagePlugin extends JavaPlugin {
  private CharacterStageService stageService;
  private ProfileBackendAdapter profileBackendAdapter;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    this.profileBackendAdapter = new MmoProfilesReflectionAdapter();
    reloadStageService();
    getServer().getPluginManager().registerEvents(new JoinStageListener(stageService), this);
    getServer().getPluginManager().registerEvents(new StageInteractListener(stageService), this);
    var command = getCommand("characterstage");
    if (command != null) {
      var stageCommand = new StageCommand(this);
      command.setExecutor(stageCommand);
      command.setTabCompleter(stageCommand);
    }
  }

  void reloadStageService() {
    reloadConfig();
    this.stageService = new CharacterStageService(this, StageConfig.load(this), profileBackendAdapter);
  }

  public CharacterStageService stageService() {
    return stageService;
  }
}
