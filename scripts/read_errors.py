import re
import sys

with open('build_errs_now.txt', encoding='utf-8', errors='replace') as f:
    raw = f.read()

# Strip ANSI escape sequences  
ansi_escape = re.compile(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')
content = ansi_escape.sub('', raw)
# Strip \r
content = content.replace('\r', '')

# Write clean version
with open('build_clean.txt', 'w', encoding='ascii', errors='replace') as f:
    f.write(content)

# Now find blocks starting with "e: file:///"
# The errors span multiple lines because the terminal wraps
# Let's just find lines with key error descriptions
lines = content.split('\n')

errors = []
i = 0
in_error = False
current_error = []
while i < len(lines):
    line = lines[i]
    if line.startswith('e: file:///'):
        if current_error:
            errors.append(' '.join(current_error))
        current_error = [line]
        in_error = True
    elif in_error and not line.startswith('>') and not line.startswith('w:') and line.strip():
        current_error.append(line.strip())
    elif line.startswith('w:') or line.startswith('>') or not line.strip():
        if current_error:
            errors.append(' '.join(current_error))
            current_error = []
        in_error = False
    i += 1

if current_error:
    errors.append(' '.join(current_error))

with open('errors_parsed.txt', 'w', encoding='ascii', errors='replace') as f:
    for i, e in enumerate(errors):
        f.write(f"{i+1}: {e}\n")

print(f"Parsed {len(errors)} errors into errors_parsed.txt")
