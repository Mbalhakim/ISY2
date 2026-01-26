package experiments;

import core.Game;
import core.Board;
import enums.Color;
import strategies.MinimaxStrategy;
import strategies.RandomStrategy;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Experiment runner voor Minimax diepte analyse.
 * Test verschillende zoekdieptes tegen Random AI en verzamelt prestatie data.
 */
public class MinimaxDepthExperiment {
    // Experiment configuratie
    private static final int GAMES_PER_DEPTH = 100;
    private static final int[] DEPTHS_TO_TEST = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final String OUTPUT_DIR = "results";
    private static final String OUTPUT_FILE = OUTPUT_DIR + "/minimax_depth_analysis.csv";
    private static final String SUMMARY_FILE = OUTPUT_DIR + "/summary_statistics.csv";
    private static final String LOG_FILE = OUTPUT_DIR + "/experiment_log.txt";

    // Progress tracking
    private static int totalGames = 0;
    private static int completedGames = 0;
    private static long experimentStartTime;

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║   MINIMAX DEPTH EXPERIMENT - OTHELLO AI RESEARCH          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();

        // Setup
        setupExperiment();

        // Run experiment
        experimentStartTime = System.currentTimeMillis();
        List<DepthStatistics> allStats = new ArrayList<>();

        for (int depth : DEPTHS_TO_TEST) {
            System.out.println("═".repeat(60));
            System.out.println("🎯 Testing Minimax Depth: " + depth);
            System.out.println("═".repeat(60));

            DepthStatistics stats = runDepthExperiment(depth);
            allStats.add(stats);

            // Print intermediate results
            printDepthSummary(stats);
            System.out.println();
        }

        // Generate final summary
        generateSummaryReport(allStats);

        long totalTime = System.currentTimeMillis() - experimentStartTime;
        System.out.println("\n✅ Experiment completed in " + formatDuration(totalTime));
        System.out.println("📊 Results saved to: " + OUTPUT_FILE);
        System.out.println("📈 Summary saved to: " + SUMMARY_FILE);
    }

    /**
     * Setup experiment: create directories and initialize CSV file
     */
    private static void setupExperiment() {
        try {
            // Create results directory
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            Files.createDirectories(Paths.get(OUTPUT_DIR + "/backup"));

            // Initialize CSV with header
            try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE))) {
                writer.println("game_id,minimax_depth,minimax_color,random_color,winner," +
                        "minimax_score,random_score,score_difference,total_moves," +
                        "game_duration_ms,avg_move_time_ms,max_move_time_ms,timestamp");
            }

            // Initialize log file
            logMessage("Experiment started");
            logMessage("Configuration: " + GAMES_PER_DEPTH + " games per depth");
            logMessage("Depths to test: " + java.util.Arrays.toString(DEPTHS_TO_TEST));

            totalGames = DEPTHS_TO_TEST.length * GAMES_PER_DEPTH;
            System.out.println("✅ Setup complete");
            System.out.println("📝 Total games to play: " + totalGames);
            System.out.println("💾 Output file: " + OUTPUT_FILE);
            System.out.println();

        } catch (IOException e) {
            System.err.println("❌ Error during setup: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Run all games for a specific depth
     */
    private static DepthStatistics runDepthExperiment(int depth) {
        DepthStatistics stats = new DepthStatistics(depth);
        long depthStartTime = System.currentTimeMillis();

        for (int gameNum = 1; gameNum <= GAMES_PER_DEPTH; gameNum++) {
            // Alternate colors for fairness
            boolean minimaxIsBlack = (gameNum % 2 == 1);

            GameResult result = runSingleGame(depth, minimaxIsBlack, gameNum);
            stats.addResult(result);

            // Write to CSV immediately
            writeGameResultToCSV(result);

            completedGames++;

            // Progress update every 10 games
            if (gameNum % 10 == 0) {
                printProgress(depth, gameNum, GAMES_PER_DEPTH);
            }
        }

        long depthDuration = System.currentTimeMillis() - depthStartTime;
        stats.setTotalDuration(depthDuration);

        return stats;
    }

    /**
     * Run a single game between Minimax and Random
     */
    private static GameResult runSingleGame(int depth, boolean minimaxIsBlack, int gameNumber) {
        long gameStartTime = System.currentTimeMillis();

        // Create strategies
        MinimaxStrategy minimaxStrategy = new MinimaxStrategy(depth);
        RandomStrategy randomStrategy = new RandomStrategy();

        // Create game with appropriate player order
        Game game;
        if (minimaxIsBlack) {
            game = new Game(minimaxStrategy, randomStrategy);
        } else {
            game = new Game(randomStrategy, minimaxStrategy);
        }

        // Track Minimax move times
        List<Long> minimaxMoveTimes = new ArrayList<>();
        long maxMoveTime = 0;

        // Play the game
        while (!game.isGameOver()) {
            Color currentPlayer = game.getCurrentPlayer();

            // Check if it's Minimax's turn
            boolean isMinimaxTurn = (minimaxIsBlack && currentPlayer == Color.BLACK) ||
                                    (!minimaxIsBlack && currentPlayer == Color.WHITE);

            if (isMinimaxTurn) {
                // Time the Minimax move
                long moveStart = System.nanoTime();
                game.playTurn();
                long moveEnd = System.nanoTime();

                long moveTimeMs = (moveEnd - moveStart) / 1_000_000;
                minimaxMoveTimes.add(moveTimeMs);
                maxMoveTime = Math.max(maxMoveTime, moveTimeMs);
            } else {
                // Random player move (don't time this)
                game.playTurn();
            }
        }

        long gameDuration = System.currentTimeMillis() - gameStartTime;

        // Calculate average Minimax move time
        double avgMoveTime = minimaxMoveTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        // Get final scores
        Board board = game.getBoard();
        int blackScore = game.getScore(Color.BLACK);
        int whiteScore = game.getScore(Color.WHITE);

        // Determine scores for Minimax and Random
        int minimaxScore = minimaxIsBlack ? blackScore : whiteScore;
        int randomScore = minimaxIsBlack ? whiteScore : blackScore;

        // Determine winner
        String winner;
        if (blackScore > whiteScore) {
            winner = minimaxIsBlack ? "MINIMAX" : "RANDOM";
        } else if (whiteScore > blackScore) {
            winner = minimaxIsBlack ? "RANDOM" : "MINIMAX";
        } else {
            winner = "DRAW";
        }

        // Create result object
        int gameId = (depth - 1) * GAMES_PER_DEPTH + gameNumber;
        return new GameResult(
                gameId,
                depth,
                minimaxIsBlack ? "BLACK" : "WHITE",
                minimaxIsBlack ? "WHITE" : "BLACK",
                winner,
                minimaxScore,
                randomScore,
                minimaxScore - randomScore,
                board.getMoveCount(),
                gameDuration,
                avgMoveTime,
                maxMoveTime
        );
    }

    /**
     * Write a single game result to CSV
     */
    private static void writeGameResultToCSV(GameResult result) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_FILE, true))) {
            writer.println(result.toCSV());
        } catch (IOException e) {
            System.err.println("⚠️ Warning: Could not write game result: " + e.getMessage());
        }
    }

    /**
     * Print progress bar
     */
    private static void printProgress(int depth, int current, int total) {
        int percentage = (current * 100) / total;
        int overallPercentage = (completedGames * 100) / totalGames;

        String bar = "█".repeat(percentage / 5) + "░".repeat(20 - percentage / 5);

        System.out.printf("\r  Depth %d: [%s] %d/%d (%d%%) | Overall: %d%%",
                depth, bar, current, total, percentage, overallPercentage);

        if (current == total) {
            System.out.println(" ✓");
        }
    }

    /**
     * Print summary statistics for a depth
     */
    private static void printDepthSummary(DepthStatistics stats) {
        System.out.println("\n📊 Results for Depth " + stats.depth + ":");
        System.out.println("   Win Rate:       " + String.format("%.1f%%", stats.getWinPercentage()));
        System.out.println("   Loss Rate:      " + String.format("%.1f%%", stats.getLossPercentage()));
        System.out.println("   Draw Rate:      " + String.format("%.1f%%", stats.getDrawPercentage()));
        System.out.println("   Avg Score Diff: " + String.format("%.2f", stats.getAvgScoreDifference()));
        System.out.println("   Avg Move Time:  " + String.format("%.0f ms", stats.getAvgMoveTime()));
        System.out.println("   Max Move Time:  " + stats.getMaxMoveTime() + " ms");
        System.out.println("   Total Duration: " + formatDuration(stats.getTotalDuration()));
    }

    /**
     * Generate summary report with statistics per depth
     */
    private static void generateSummaryReport(List<DepthStatistics> allStats) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SUMMARY_FILE))) {
            // Write header
            writer.println("depth,total_games,wins,losses,draws,win_percentage,loss_percentage," +
                    "draw_percentage,avg_score_diff,std_score_diff,avg_move_time_ms," +
                    "max_move_time_ms,total_duration_ms");

            // Write statistics for each depth
            for (DepthStatistics stats : allStats) {
                writer.println(stats.toCSV());
            }

            System.out.println("\n" + "═".repeat(60));
            System.out.println("📈 FINAL SUMMARY");
            System.out.println("═".repeat(60));

            System.out.println("\n| Depth | Win% | Loss% | Draw% | Avg Score Diff | Avg Time (ms) |");
            System.out.println("|-------|------|-------|-------|----------------|---------------|");

            for (DepthStatistics stats : allStats) {
                System.out.printf("|   %d   | %5.1f | %5.1f | %5.1f |     %6.2f     |    %8.0f   |\n",
                        stats.depth,
                        stats.getWinPercentage(),
                        stats.getLossPercentage(),
                        stats.getDrawPercentage(),
                        stats.getAvgScoreDifference(),
                        stats.getAvgMoveTime());
            }
            System.out.println();

        } catch (IOException e) {
            System.err.println("❌ Error writing summary: " + e.getMessage());
        }
    }

    /**
     * Log message to log file
     */
    private static void logMessage(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            writer.println("[" + timestamp + "] " + message);
        } catch (IOException e) {
            // Silently fail for logging
        }
    }

    /**
     * Format duration in human readable format
     */
    private static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Inner class to hold a single game result
     */
    static class GameResult {
        int gameId;
        int minimaxDepth;
        String minimaxColor;
        String randomColor;
        String winner;
        int minimaxScore;
        int randomScore;
        int scoreDifference;
        int totalMoves;
        long gameDurationMs;
        double avgMoveTimeMs;
        long maxMoveTimeMs;
        String timestamp;

        public GameResult(int gameId, int minimaxDepth, String minimaxColor, String randomColor,
                          String winner, int minimaxScore, int randomScore, int scoreDifference,
                          int totalMoves, long gameDurationMs, double avgMoveTimeMs, long maxMoveTimeMs) {
            this.gameId = gameId;
            this.minimaxDepth = minimaxDepth;
            this.minimaxColor = minimaxColor;
            this.randomColor = randomColor;
            this.winner = winner;
            this.minimaxScore = minimaxScore;
            this.randomScore = randomScore;
            this.scoreDifference = scoreDifference;
            this.totalMoves = totalMoves;
            this.gameDurationMs = gameDurationMs;
            this.avgMoveTimeMs = avgMoveTimeMs;
            this.maxMoveTimeMs = maxMoveTimeMs;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public String toCSV() {
            return String.format("%d,%d,%s,%s,%s,%d,%d,%d,%d,%d,%.2f,%d,%s",
                    gameId, minimaxDepth, minimaxColor, randomColor, winner,
                    minimaxScore, randomScore, scoreDifference, totalMoves,
                    gameDurationMs, avgMoveTimeMs, maxMoveTimeMs, timestamp);
        }
    }

    /**
     * Inner class to hold statistics for a specific depth
     */
    static class DepthStatistics {
        int depth;
        int totalGames = 0;
        int wins = 0;
        int losses = 0;
        int draws = 0;
        List<Integer> scoreDifferences = new ArrayList<>();
        List<Double> moveTimes = new ArrayList<>();
        long maxMoveTime = 0;
        long totalDuration = 0;

        public DepthStatistics(int depth) {
            this.depth = depth;
        }

        public void addResult(GameResult result) {
            totalGames++;

            if (result.winner.equals("MINIMAX")) {
                wins++;
            } else if (result.winner.equals("RANDOM")) {
                losses++;
            } else {
                draws++;
            }

            scoreDifferences.add(result.scoreDifference);
            moveTimes.add(result.avgMoveTimeMs);
            maxMoveTime = Math.max(maxMoveTime, result.maxMoveTimeMs);
        }

        public void setTotalDuration(long duration) {
            this.totalDuration = duration;
        }

        public double getWinPercentage() {
            return (wins * 100.0) / totalGames;
        }

        public double getLossPercentage() {
            return (losses * 100.0) / totalGames;
        }

        public double getDrawPercentage() {
            return (draws * 100.0) / totalGames;
        }

        public double getAvgScoreDifference() {
            return scoreDifferences.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        }

        public double getStdScoreDifference() {
            double avg = getAvgScoreDifference();
            double variance = scoreDifferences.stream()
                    .mapToDouble(x -> Math.pow(x - avg, 2))
                    .average()
                    .orElse(0.0);
            return Math.sqrt(variance);
        }

        public double getAvgMoveTime() {
            return moveTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        public long getMaxMoveTime() {
            return maxMoveTime;
        }

        public long getTotalDuration() {
            return totalDuration;
        }

        public String toCSV() {
            return String.format("%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d",
                    depth, totalGames, wins, losses, draws,
                    getWinPercentage(), getLossPercentage(), getDrawPercentage(),
                    getAvgScoreDifference(), getStdScoreDifference(),
                    getAvgMoveTime(), maxMoveTime, totalDuration);
        }
    }
}

