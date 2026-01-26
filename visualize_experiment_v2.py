import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from pathlib import Path

# Configuratie
sns.set_style("whitegrid")
plt.rcParams['figure.figsize'] = (14, 10)
results_dir = Path(__file__).parent / "results"

# Laad de data
summary_stats = pd.read_csv(results_dir / "summary_statistics_fixed.csv")
detailed_data = pd.read_csv(results_dir / "minimax_depth_analysis.csv")

# Data opschoning voor detailed_data
for col in ['minimax_depth', 'minimax_score', 'random_score', 'score_difference', 'game_duration_ms', 'avg_move_time_ms']:
    if col in detailed_data.columns:
        detailed_data[col] = pd.to_numeric(detailed_data[col], errors='coerce')

print("Minimax Depth Experiment Visualisatie")
print("=" * 50)
print(f"Dieptes geanalyseerd: {sorted(summary_stats['depth'].unique())}")
print(f"Totaal games per diepte: {summary_stats['total_games'].iloc[0]}")
print(f"Totaal games: {summary_stats['total_games'].sum()}")
print()

# Maak een figure met 7 subplots
fig = plt.figure(figsize=(18, 14))

# 1. Winpercentage per diepte
ax1 = plt.subplot(3, 3, 1)
colors = ['#2ecc71' if x >= 50 else '#e74c3c' for x in summary_stats['win_percentage']]
ax1.bar(summary_stats['depth'], summary_stats['win_percentage'], color=colors, edgecolor='black', linewidth=1.5)
ax1.axhline(y=50, color='gray', linestyle='--', alpha=0.5, label='50% threshold')
ax1.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax1.set_ylabel('Winpercentage (%)', fontsize=10, fontweight='bold')
ax1.set_title('1. Winpercentage per Diepte', fontsize=11, fontweight='bold')
ax1.set_ylim(0, 105)
ax1.grid(axis='y', alpha=0.3)
for i, v in enumerate(summary_stats['win_percentage']):
    ax1.text(summary_stats['depth'].iloc[i], v + 2, f'{v:.1f}%', ha='center', fontsize=9, fontweight='bold')

# 2. Win/Loss/Draw verdeling
ax2 = plt.subplot(3, 3, 2)
x = np.arange(len(summary_stats['depth']))
width = 0.25
ax2.bar(x - width, summary_stats['win_percentage'], width, label='Wins', color='#2ecc71', edgecolor='black')
ax2.bar(x, summary_stats['loss_percentage'], width, label='Losses', color='#e74c3c', edgecolor='black')
ax2.bar(x + width, summary_stats['draw_percentage'], width, label='Draws', color='#3498db', edgecolor='black')
ax2.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax2.set_ylabel('Percentage (%)', fontsize=10, fontweight='bold')
ax2.set_title('2. Win/Loss/Draw Verdeling', fontsize=11, fontweight='bold')
ax2.set_xticks(x)
ax2.set_xticklabels(summary_stats['depth'])
ax2.legend(loc='upper left', fontsize=9)
ax2.grid(axis='y', alpha=0.3)

# 3. Gemiddelde score verschil
ax3 = plt.subplot(3, 3, 3)
colors_score = ['#2ecc71' if x > 0 else '#e74c3c' for x in summary_stats['avg_score_diff']]
ax3.bar(summary_stats['depth'], summary_stats['avg_score_diff'], color=colors_score, edgecolor='black', linewidth=1.5)
ax3.axhline(y=0, color='black', linestyle='-', linewidth=1)
ax3.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax3.set_ylabel('Gemiddelde Score Verschil', fontsize=10, fontweight='bold')
ax3.set_title('3. Gemiddelde Score Verschil (Minimax - Random)', fontsize=11, fontweight='bold')
ax3.grid(axis='y', alpha=0.3)
for i, v in enumerate(summary_stats['avg_score_diff']):
    ax3.text(summary_stats['depth'].iloc[i], v + (2 if v > 0 else -2), f'{v:.1f}', ha='center', fontsize=9, fontweight='bold')

# 4. Rekentijd per zet (logaritmisch)
ax4 = plt.subplot(3, 3, 4)
ax4.semilogy(summary_stats['depth'], summary_stats['avg_move_time_ms'], marker='o', linewidth=2.5, 
             markersize=8, color='#9b59b6', markerfacecolor='#e74c3c', markeredgewidth=2, markeredgecolor='#9b59b6')
ax4.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax4.set_ylabel('Gemiddelde Zettijd (ms) - Log Schaal', fontsize=10, fontweight='bold')
ax4.set_title('4. Rekentijd per Zet (Logaritmisch)', fontsize=11, fontweight='bold')
ax4.grid(True, alpha=0.3, which='both')
for i, v in enumerate(summary_stats['avg_move_time_ms']):
    if v > 0:
        ax4.text(summary_stats['depth'].iloc[i] + 0.15, v * 1.3, f'{v:.1f}ms', fontsize=8, fontweight='bold')

# 5. Totale spelduur
ax5 = plt.subplot(3, 3, 5)
ax5.bar(summary_stats['depth'], summary_stats['total_duration_ms'] / 1000, 
        color='#3498db', edgecolor='black', linewidth=1.5)
ax5.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax5.set_ylabel('Totale Duur (seconden)', fontsize=10, fontweight='bold')
ax5.set_title('5. Totale Spelduur (100 games)', fontsize=11, fontweight='bold')
ax5.grid(axis='y', alpha=0.3)
for i, v in enumerate(summary_stats['total_duration_ms']):
    ax5.text(summary_stats['depth'].iloc[i], v / 1000 + 100, f'{v/1000:.1f}s', ha='center', fontsize=9, fontweight='bold')

# 6. Diminishing Returns Analyse (Win% vs Rekentijd)
ax6 = plt.subplot(3, 3, 6)
ax6_twin = ax6.twinx()
line1 = ax6.plot(summary_stats['depth'], summary_stats['win_percentage'], marker='o', linewidth=2.5, 
                 markersize=8, color='#2ecc71', label='Winpercentage', markeredgewidth=2)
line2 = ax6_twin.semilogy(summary_stats['depth'], summary_stats['avg_move_time_ms'], marker='s', linewidth=2.5, 
                           markersize=8, color='#e74c3c', label='Rekentijd', markerfacecolor='#f39c12', markeredgewidth=2)
ax6.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax6.set_ylabel('Winpercentage (%)', fontsize=10, fontweight='bold', color='#2ecc71')
ax6_twin.set_ylabel('Rekentijd (ms) - Log Schaal', fontsize=10, fontweight='bold', color='#e74c3c')
ax6.set_title('6. Diminishing Returns Analyse', fontsize=11, fontweight='bold')
ax6.tick_params(axis='y', labelcolor='#2ecc71')
ax6_twin.tick_params(axis='y', labelcolor='#e74c3c')
ax6.grid(True, alpha=0.3)
lines = line1 + line2
labels = [l.get_label() for l in lines]
ax6.legend(lines, labels, loc='center right', fontsize=9)

# 7. Performance Improvement per Diepte
ax7 = plt.subplot(3, 3, 7)
win_diff = summary_stats['win_percentage'].diff()
ax7.bar(summary_stats['depth'][1:], win_diff[1:], color='#16a085', edgecolor='black', linewidth=1.5)
ax7.axhline(y=0, color='black', linestyle='-', linewidth=1)
ax7.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax7.set_ylabel('Win% Verbetering', fontsize=10, fontweight='bold')
ax7.set_title('7. Incremental Performance Verbetering', fontsize=11, fontweight='bold')
ax7.set_xticks(summary_stats['depth'][1:])
ax7.grid(axis='y', alpha=0.3)
for i, v in enumerate(win_diff[1:], 1):
    ax7.text(summary_stats['depth'].iloc[i], v + (2 if v > 0 else -2), f'{v:.1f}%', ha='center', fontsize=9, fontweight='bold')

# Verwijder lege subplot
ax8 = plt.subplot(3, 3, 8)
ax8.axis('off')



plt.suptitle('Minimax Depth Experiment: Gedetailleerde Analyse', 
             fontsize=14, fontweight='bold', y=0.995)
plt.tight_layout()
plt.savefig(results_dir / "minimax_depth_analysis_visualizations.png", dpi=300, bbox_inches='tight')
print("Visualisatie opgeslagen: minimax_depth_analysis_visualizations.png")

# Aanvullende single-plot visualisaties
fig2, axes = plt.subplots(2, 2, figsize=(14, 10))

# Heatmap van win percentages
ax = axes[0, 0]
heatmap_data = summary_stats[['depth', 'win_percentage']].set_index('depth').T
sns.heatmap(heatmap_data, annot=True, fmt='.1f', cmap='RdYlGn', center=50, 
            cbar_kws={'label': 'Win %'}, ax=ax, linewidths=2)
ax.set_title('Winpercentage Heatmap per Diepte', fontsize=11, fontweight='bold')

# Rekentijd exponentiële groei
ax = axes[0, 1]
valid_data = summary_stats[summary_stats['avg_move_time_ms'] > 0].copy()
if len(valid_data) > 1:
    ax.scatter(valid_data['depth'], valid_data['avg_move_time_ms'], 
              s=200, alpha=0.6, color='#e74c3c', edgecolor='black', linewidth=2)
    try:
        # Gebruik exponentiële fit
        z = np.polyfit(valid_data['depth'].values, np.log(valid_data['avg_move_time_ms'].values), 1)
        p = np.poly1d(z)
        x_smooth = np.linspace(valid_data['depth'].min(), valid_data['depth'].max(), 100)
        y_smooth = np.exp(p(x_smooth))
        ax.plot(x_smooth, y_smooth, 'b--', linewidth=2, label='Exponentieel trend')
        ax.legend()
    except Exception as e:
        pass
ax.set_xlabel('Minimax Diepte', fontsize=10, fontweight='bold')
ax.set_ylabel('Rekentijd (ms)', fontsize=10, fontweight='bold')
ax.set_title('Rekentijd Exponentiële Groei', fontsize=11, fontweight='bold')
ax.grid(True, alpha=0.3)

# Score verdeling box plot
ax = axes[1, 0]
score_diffs = []
depths = []
for depth in sorted(summary_stats['depth'].unique()):
    depth_data = detailed_data[detailed_data['minimax_depth'] == depth]
    score_diff = pd.to_numeric(depth_data['score_difference'], errors='coerce')
    if len(score_diff) > 0:
        score_diffs.append(score_diff.dropna())
        depths.append(f'D{int(depth)}')
if len(score_diffs) > 0:
    ax.boxplot(score_diffs, tick_labels=depths)
    ax.axhline(y=0, color='red', linestyle='--', linewidth=2, alpha=0.5)
    ax.set_ylabel('Score Verschil (Minimax - Random)', fontsize=10, fontweight='bold')
    ax.set_title('Score Verschil Verdeling per Diepte', fontsize=11, fontweight='bold')
    ax.grid(axis='y', alpha=0.3)

# Correlatie matrix
ax = axes[1, 1]
correlation_data = summary_stats[['depth', 'win_percentage', 'avg_move_time_ms', 'avg_score_diff']].copy()
correlation_data['avg_move_time_ms_log'] = np.log(correlation_data['avg_move_time_ms'] + 1)
corr_matrix = correlation_data[['depth', 'win_percentage', 'avg_move_time_ms_log', 'avg_score_diff']].corr()
sns.heatmap(corr_matrix, annot=True, fmt='.2f', cmap='coolwarm', center=0, 
            square=True, ax=ax, cbar_kws={'label': 'Correlatie'}, linewidths=1)
ax.set_title('Correlatie Matrix', fontsize=11, fontweight='bold')

plt.tight_layout()
plt.savefig(results_dir / "minimax_depth_additional_analysis.png", dpi=300, bbox_inches='tight')
print("Aanvullende visualisatie opgeslagen: minimax_depth_additional_analysis.png")

# Print gedetailleerde statistieken
print("\n" + "=" * 80)
print("GEDETAILLEERDE STATISTIEKEN:")
print("=" * 80)
print(summary_stats.to_string(index=False))
print("\n" + "=" * 80)
print("Alle visualisaties zijn gegenereerd in de 'results' map!")
print("=" * 80)
