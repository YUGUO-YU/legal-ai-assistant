"""
legal_mcp - MCP server for Legal AI Assistant backend APIs.

Exposes legal search, case similarity, company query, contract review,
and other AI-powered legal tools as MCP tools.
"""

from .server import mcp

__all__ = ["mcp"]
