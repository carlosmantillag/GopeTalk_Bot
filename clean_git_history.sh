#!/bin/bash

# Script para limpiar archivos grandes del historial de Git
# ADVERTENCIA: Este script reescribe el historial de Git

echo "🧹 Limpiando archivos grandes del historial de Git..."
echo ""
echo "⚠️  ADVERTENCIA: Esto reescribirá el historial de Git"
echo "⚠️  Asegúrate de tener un backup antes de continuar"
echo ""
read -p "¿Deseas continuar? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "❌ Operación cancelada"
    exit 1
fi

echo ""
echo "📦 Eliminando archivos grandes del historial..."

# Eliminar archivos específicos del historial
git filter-branch --force --index-filter \
  'git rm -r --cached --ignore-unmatch caches/ wrapper/ .gradle/ gradle/wrapper/' \
  --prune-empty --tag-name-filter cat -- --all

echo ""
echo "🗑️  Limpiando referencias..."
rm -rf .git/refs/original/
git reflog expire --expire=now --all
git gc --prune=now --aggressive

echo ""
echo "✅ Limpieza completada"
echo ""
echo "📝 Próximos pasos:"
echo "1. Verifica que todo esté bien: git log --oneline"
echo "2. Fuerza el push: git push origin clean_version_wav --force"
echo ""
echo "⚠️  NOTA: Usa --force con precaución, esto reescribirá el historial remoto"
