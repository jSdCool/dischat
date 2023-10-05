# Dischat
minecraft mod that connects minecraft chat with a discord channel

this mod uses the fabric loader

# how to set up:

1) install the mod on you server and run it
2) find the config file
   * config/dischat.cfg
3) copy and paste you bot token into the line that says #botToken=
4) copy the ID of the discord server and the channel that you want to link to this server
   * to obtain ID's on discord enable developermode
5) restart your server and all should work

note: no tutorial for creating a discord bot is given here  
discord bots requires the MESSAGE_CONTENT and GUILD_MEMBERS intents

# Commands

commands that can be used in discord:  

- /help ------- list all commands for discord  
- /list ------- list all player that are online  
- /version ---- display the version of the mod and minecraft   

 moderator commands for discord  
- /tp -------- teleport a player  
- /pos ------- get the position of a player  
- /kickMC ---- kick a player  
- /gamemode -- change the game mode of a player  

commands that can be used in minecraft (requires op level 3)  

/dischat reload ------- reload the discord bot    
/dischat auth --------- authorize a discord user  
/dischat unAuth ------- remove an authorized discord user  
/dischat status ------- change the current status of the bot  
