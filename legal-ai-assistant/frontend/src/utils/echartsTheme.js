export const legalAITheme = {
  color: [
    '#667eea',
    '#764ba2',
    '#f56c6c',
    '#e6a23c',
    '#67c23a',
    '#409eff',
    '#909399',
    '#c71585',
  ],
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: 'Inter, "Chinese PingFang SC", "Microsoft YaHei", sans-serif'
  },
  title: {
    textStyle: {
      color: '#1f2937',
      fontWeight: 600,
      fontSize: 16
    },
    subtextStyle: {
      color: '#6b7280',
      fontSize: 12
    }
  },
  legend: {
    textStyle: {
      color: '#6b7280'
    }
  },
  tooltip: {
    backgroundColor: 'rgba(255,255,255,0.95)',
    borderColor: '#e5e7eb',
    borderWidth: 1,
    textStyle: {
      color: '#1f2937'
    },
    extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.1); border-radius: 8px;'
  },
  categoryAxis: {
    axisLine: {
      lineStyle: {
        color: '#e5e7eb'
      }
    },
    axisTick: {
      lineStyle: {
        color: '#e5e7eb'
      }
    },
    axisLabel: {
      color: '#6b7280'
    }
  },
  valueAxis: {
    axisLine: {
      show: false
    },
    splitLine: {
      lineStyle: {
        color: '#f3f4f6',
        type: 'dashed'
      }
    }
  }
}

export const legalAIDarkTheme = {
  ...legalAITheme,
  color: [
    '#7c8cf8',
    '#9d7cd4',
    '#f56c6c',
    '#e6a23c',
    '#7ee787',
    '#58a6ff',
    '#8b949e',
    '#f778ba',
  ],
  backgroundColor: 'transparent',
  textStyle: {
    ...legalAITheme.textStyle,
    color: '#e6edf3'
  },
  title: {
    textStyle: {
      color: '#e6edf3',
      fontWeight: 600,
      fontSize: 16
    },
    subtextStyle: {
      color: '#8b949e',
      fontSize: 12
    }
  },
  legend: {
    textStyle: {
      color: '#8b949e'
    }
  },
  tooltip: {
    backgroundColor: 'rgba(26,31,38,0.95)',
    borderColor: '#30363d',
    textStyle: {
      color: '#e6edf3'
    }
  },
  categoryAxis: {
    axisLine: {
      lineStyle: {
        color: '#30363d'
      }
    },
    axisTick: {
      lineStyle: {
        color: '#30363d'
      }
    },
    axisLabel: {
      color: '#8b949e'
    }
  },
  valueAxis: {
    axisLine: {
      show: false
    },
    splitLine: {
      lineStyle: {
        color: '#21262d',
        type: 'dashed'
      }
    }
  }
}

export const createChartOption = (type, data, settings = {}) => {
  const baseOptions = {
    tooltip: {
      trigger: 'item',
      ...settings.tooltip
    },
    legend: {
      ...settings.legend
    },
    color: legalAITheme.color,
    ...settings.base
  }

  switch (type) {
    case 'line':
      return {
        ...baseOptions,
        xAxis: {
          type: 'category',
          data: data.xAxis,
          ...legalAITheme.categoryAxis
        },
        yAxis: {
          type: 'value',
          ...legalAITheme.valueAxis
        },
        series: [{
          data: data.series,
          type: 'line',
          smooth: true,
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(102,126,234,0.3)' },
                { offset: 1, color: 'rgba(102,126,234,0.05)' }
              ]
            }
          },
          lineStyle: {
            color: '#667eea',
            width: 2
          },
          itemStyle: {
            color: '#667eea'
          }
        }]
      }

    case 'bar':
      return {
        ...baseOptions,
        xAxis: {
          type: 'category',
          data: data.xAxis,
          ...legalAITheme.categoryAxis
        },
        yAxis: {
          type: 'value',
          ...legalAITheme.valueAxis
        },
        series: [{
          data: data.series,
          type: 'bar',
          barWidth: '60%',
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: '#667eea' },
                { offset: 1, color: '#764ba2' }
              ]
            }
          }
        }]
      }

    case 'pie':
      return {
        ...baseOptions,
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          data: data.series,
          label: {
            color: '#6b7280'
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0,0,0,0.2)'
            }
          }
        }]
      }

    default:
      return baseOptions
  }
}
