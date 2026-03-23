import os
import re

base_dir = r'd:\HyLee_IT\Kotlin\finfit\app\src\main\java\com\example\finfit\finance\ui'

def update_file(path, new_package, imports):
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    added_imports = False
    for line in lines:
        # Normalize package
        if line.strip().startswith('package '):
            new_lines.append(f'package {new_package}\n')
            if not added_imports:
                new_lines.append('\n')
                for imp in imports:
                    new_lines.append(f'import {imp}\n')
                added_imports = True
        elif line.strip().startswith('import '):
             new_lines.append(line)
        else:
             new_lines.append(line)
            
    with open(path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

# Utils (no extra imports needed from ui)
update_file(os.path.join(base_dir, 'utils', 'FinanceUtils.kt'), 'com.example.finfit.finance.ui.utils', [])

# Wrappers
update_file(os.path.join(base_dir, 'wrappers', 'FinanceWrappers.kt'), 'com.example.finfit.finance.ui.wrappers', [
    'com.example.finfit.finance.ui.screens.*',
    'com.example.finfit.finance.ui.utils.*'
])

# Navigation
update_file(os.path.join(base_dir, 'navigation', 'FinanceNavGraph.kt'), 'com.example.finfit.finance.ui.navigation', [
    'com.example.finfit.finance.ui.screens.*',
    'com.example.finfit.finance.ui.wrappers.*',
    'com.example.finfit.finance.ui.utils.*'
])

# Screens
screens_dir = os.path.join(base_dir, 'screens')
for filename in os.listdir(screens_dir):
    if filename.endswith('.kt'):
        update_file(os.path.join(screens_dir, filename), 'com.example.finfit.finance.ui.screens', [
            'com.example.finfit.finance.ui.utils.*',
            'com.example.finfit.finance.ui.wrappers.*'
        ])
