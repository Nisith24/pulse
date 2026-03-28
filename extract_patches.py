import os
import re

input_file = r'c:\CodeStuffs\References\pulse\pr_diffs_summary.txt'
output_file = r'c:\CodeStuffs\References\pulse\clean_patch.diff'

valid_extensions = ('.kt', '.kts', '.xml')

with open(input_file, 'r', encoding='utf-8') as f:
    lines = f.readlines()

out_lines = []
current_diff = []
keep_current = False

for line in lines:
    if line.startswith('diff --git '):
        if current_diff and keep_current:
            out_lines.extend(current_diff)
        
        current_diff = [line]
        
        # Determine if we should keep this diff
        filepath = line.strip().split(' b/')[-1]
        keep_current = filepath.endswith(valid_extensions)
    elif current_diff is not None:
        current_diff.append(line)

if current_diff and keep_current:
    out_lines.extend(current_diff)

with open(output_file, 'w', encoding='utf-8') as f:
    f.writelines(out_lines)

print(f"Extracted {len(out_lines)} lines to {output_file}")
