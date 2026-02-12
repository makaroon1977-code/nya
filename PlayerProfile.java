public class PlayerProfile {
    private String playerName;
    private int playerLevel;
    private int playerScore;
    private String playerStatus;

    public PlayerProfile(String playerName, int playerLevel, int playerScore, String playerStatus) {
        this.playerName = playerName;
        this.playerLevel = playerLevel;
        this.playerScore = playerScore;
        this.playerStatus = playerStatus;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPlayerLevel() {
        return playerLevel;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public String getPlayerStatus() {
        return playerStatus;
    }

    public void levelUp() {
        playerLevel++;
    }

    public void updateScore(int score) {
        playerScore += score;
    }

    public void updateStatus(String status) {
        playerStatus = status;
    }

    @Override
    public String toString() {
        return "PlayerProfile { " + 
               "Name: '" + playerName + '\'' + 
               ", Level: " + playerLevel + 
               ", Score: " + playerScore + 
               ", Status: '" + playerStatus + '\'' + 
               " }";
    }
}